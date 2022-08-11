package com.migrator;

public enum DeckOrderItemHeaders implements CSVHeaderInterface{
     ORDERNUMBER("OrderNumber"),
     ITEMSTATUSCODE("ItemStatusCode"),
     UPC("UPC"),
     PRODUCTCODE("ProductCode"),
     SKU("SKU"),
     QUANTITY("Quantity"),
     SIZE("Size"),
     ATTRIBUTE("Attribute"),
     IMAGEURL("ImageURL"),
     NETPRICE("NetPrice"),
     GROSSPRICE("GrossPrice"),
     DISCOUNTAMOUNT("DiscountAmount"),
     DISCOUNTCODE("DiscountCode"),
     IS_LCP("is_lcp"),
     LCP_CHECK("lcp_check"),
     LCP_FOR_ITEM_ID("lcp_for_item_id"),
     LCP_TYPE("lcp_type"),
     LCP_ADD("lcp_add"),
     USSALESTAX("USSalesTax"),
     VATSALESTAX("VATSalesTax"),
     GSTSALESTAX("GSTSalesTax"),
     HSTSALESTAX("HSTSalesTax"),
     PSTSALESTAX("PSTSalesTax"),
     GSTVATSALESTAX("GSTVATSalesTax"),
     NETSALESTAX("NetSalesTax"),
     WAREHOUSECODE("WarehouseCode");

     public final String value;

     private DeckOrderItemHeaders(String label) {
          this.value = label;
     }

     public String value() {
          return value;
     }
}
