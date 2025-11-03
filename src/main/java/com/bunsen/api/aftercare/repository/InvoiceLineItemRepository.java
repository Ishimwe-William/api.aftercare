
package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.model.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, String> {

    @Query("SELECT ili FROM InvoiceLineItem ili WHERE ili.invoice.invoiceId = :invoiceId")
    List<InvoiceLineItem> findByInvoiceId(@Param("invoiceId") String invoiceId);

    @Query("SELECT ili FROM InvoiceLineItem ili WHERE ili.partId = :partId")
    List<InvoiceLineItem> findByPartId(@Param("partId") String partId);
}
