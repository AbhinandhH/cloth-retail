package com.clothingretail.inventory;

import com.clothingretail.common.NotFoundException;
import com.clothingretail.inventory.dto.PurchaseItemRequest;
import com.clothingretail.inventory.dto.PurchaseItemResponse;
import com.clothingretail.inventory.dto.PurchaseRequest;
import com.clothingretail.inventory.dto.PurchaseResponse;
import com.clothingretail.masterdata.Vendor;
import com.clothingretail.masterdata.VendorRepository;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.ProductVariantRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creating a Purchase is the one place stock increases: it writes the
 * Purchase+PurchaseItem rows, bumps each target ProductVariant.stockQuantity,
 * and appends a PURCHASE_IN InventoryTransaction per line - all in one
 * transaction so the audit log and the live stock count can never drift
 * apart.
 */
@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final VendorRepository vendorRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            VendorRepository vendorRepository,
            ProductVariantRepository productVariantRepository,
            InventoryTransactionRepository inventoryTransactionRepository) {
        this.purchaseRepository = purchaseRepository;
        this.vendorRepository = vendorRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Transactional
    public PurchaseResponse createPurchase(PurchaseRequest request) {
        Vendor vendor = vendorRepository.findById(request.vendorId())
                .orElseThrow(() -> new NotFoundException("Vendor not found: " + request.vendorId()));

        Purchase purchase = new Purchase();
        purchase.setVendor(vendor);
        purchase.setInvoiceNumber(request.invoiceNumber());

        for (PurchaseItemRequest itemRequest : request.items()) {
            ProductVariant variant = productVariantRepository.findById(itemRequest.productVariantId())
                    .orElseThrow(() -> new NotFoundException("Product variant not found: " + itemRequest.productVariantId()));

            PurchaseItem item = new PurchaseItem();
            item.setProductVariant(variant);
            item.setQuantity(itemRequest.quantity());
            item.setPurchasePrice(itemRequest.purchasePrice());
            item.setSellingPrice(itemRequest.sellingPrice());
            purchase.addItem(item);

            variant.setStockQuantity(variant.getStockQuantity() + itemRequest.quantity());
            if (itemRequest.sellingPrice() != null) {
                variant.setSellingPrice(itemRequest.sellingPrice());
            }
            productVariantRepository.save(variant);
        }

        Purchase saved = purchaseRepository.save(purchase);

        for (PurchaseItem item : saved.getItems()) {
            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setProductVariant(item.getProductVariant());
            transaction.setType(InventoryTransactionType.PURCHASE_IN);
            transaction.setQuantity(item.getQuantity());
            transaction.setReferenceType("PURCHASE");
            transaction.setReferenceId(saved.getId());
            inventoryTransactionRepository.save(transaction);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getPurchase(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase not found: " + id));
        return toResponse(purchase);
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> listPurchases() {
        return purchaseRepository.findAll().stream().map(this::toResponse).toList();
    }

    private PurchaseResponse toResponse(Purchase purchase) {
        List<PurchaseItemResponse> items = purchase.getItems().stream()
                .map(i -> new PurchaseItemResponse(
                        i.getId(), i.getProductVariant().getId(), i.getProductVariant().getSku(), i.getQuantity(), i.getPurchasePrice(), i.getSellingPrice()))
                .toList();
        return new PurchaseResponse(
                purchase.getId(), purchase.getVendor().getId(), purchase.getVendor().getName(), purchase.getInvoiceNumber(),
                purchase.getPurchaseDate(), items);
    }
}
