package com.migrator;

import java.io.IOException;

public class Mage2DeckOrdersItemsCSV extends JSONToCSV {
     public Mage2DeckOrdersItemsCSV() throws IOException{
          //signify that this is the Order items (products) section and not something else like customers
          super( "orders", "items" );
     }


}
