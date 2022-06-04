package com.migrator;

public enum DeckOrderHeaders {
    ORDERNUMBER("OrderNumber"),
    SITECODE("SiteCode"),
    CUSTOMERID("CustomerID"),
    CUSTOMERLOCALE("CustomerLocale"),
    ORDERDATE("OrderDate"),
    ORDERSTATUSCODE("OrderStatusCode"),
    DISCOUNTAMOUNT("DiscountAmount"),
    DISCOUNTCODE("DiscountCode"),
    SHIPPINGMETHOD("ShippingMethod"),
    SHIPPINGCOST("ShippingCost"),
    SHIPPINGDISCOUNTAMOUNT("ShippingDiscountAmount"),
    SHIPPINGDISCOUNTCODE("ShippingDiscountCode"),
    USSHIPPINGTAX("USShippingTax"),
    VATSHIPPINGTAX("VATShippingTax"),
    GSTSHIPPINGTAX("GSTShippingTax"),
    HSTSHIPPINGTAX("HSTShippingTax"),
    PSTSHIPPINGTAX("PSTShippingTax"),
    GSTVATSHIPPINGTAX("GSTVATShippingTax"),
    NETSHIPPINGTAX("NetShippingTax"),
    USSALESTAX("USSalesTax"),
    VATSALESTAX("VATSalesTax"),
    GSTSALESTAX("GSTSalesTax"),
    HSTSALESTAX("HSTSalesTax"),
    PSTSALESTAX("PSTSalesTax"),
    GSTVATSALESTAX("GSTVATSalesTax"),
    NETSALESTAX("NetSalesTax"),
    SUBTOTAL("Subtotal"),
    ORDERTOTAL("OrderTotal"),
    BILLFIRSTNAME("BillFirstName"),
    BILLLASTNAME("BillLastName"),
    BILLEMAIL("BillEmail"),
    BILLPHONE("BillPhone"),
    BILLADDRESS("BillAddress"),
    BILLADDRESS2("BillAddress2"),
    BILLADDRESS3("BillAddress3"),
    BILLCITY("BillCity"),
    BILLPROVINCE("BillProvince"),
    BILLPOSTALCODE("BillPostalCode"),
    BILLCOUNTRY("BillCountry"),
    SHIPFIRSTNAME("ShipFirstName"),
    SHIPLASTNAME("ShipLastName"),
    SHIPEMAIL("ShipEmail"),
    SHIPPHONE("ShipPhone"),
    SHIPADDRESS("ShipAddress"),
    SHIPADDRESS2("ShipAddress2"),
    SHIPADDRESS3("ShipAddress3"),
    SHIPCITY("ShipCity"),
    SHIPPROVINCE("ShipProvince"),
    SHIPPOSTALCODE("ShipPostalCode"),
    SHIPCOUNTRY("ShipCountry"),
    ORDERSOURCE("OrderSource");

    public final String value;

    private DeckOrderHeaders(String label) {
        this.value = label;
    }

}
