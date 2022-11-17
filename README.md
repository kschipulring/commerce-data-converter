# Commerce Data Converter

A tool to convert data from one e-commerce system to another.
Starting off with Magento to Salesforce Commerce Cloud, aka, Demandware.
Also exports to Deck Commerce.

Later on, there shall be other systems.  Also, as of November 2022, it only transfers order data.
Though it should be easy to expand in the future to things like products, customers and other data sections.

### Requires:
- Java 11.  But perhaps 8 might do.  I did not write anything that uses features beyond version 8.
However, My JDK just happened to be version 11 for editing and running.
- Maven.  Uses Maven dependencies for DOTEnv, JUnit and others.  For more information, please read the pom.xml file.
- For now, an active Magento instance for which you have API access to, as well as it having some completed orders accessible via the REST API.
- An active Salesforce Commerce Cloud or Deck Commerce instance is not technically necessary, as runs from this app only pull JSON data from the Magento source
and then renders the data to either SFCC XML format or Deck Commerce CSV format.
- half decent editor like Eclipse or Visual Studio Code.  Java extensions recommended for VSC.

### Install:
In your favorite command line tool, from within this main directory, run:

*mvn dependency:resolve*

or

*mvn install*

(I tend to use the second)

### Configuration
Maybe it's because I started with PHP, but I prefer DOTEnv over properties files.

<sup>*(* * *avoids tomatoes thrown by Java veterans* * *)*</sup>

For greater understanding of how it is used here, please browse *'.env.sample'*

Two things you will *absolutely* need to change are:

**MAGE_AUTH_TOKEN="YOUR-MAGE-AUTH-TOKEN"**

**MAGE_API_BASE_URL="https://{YOUR-MAGENTO-BASE-URL}/index.php/rest/V1/"**

... as these are not actual API credentials or an actual Magento site.


...also, you really want to consider changing:

**YOUR_COMPANY_NAME="Excelsior Industries"**

...unless of course, your business name actually is "Excelsior Industries".  Which would be a very strange coincidence.


## Running:
To generate orders, I run the src\main\java\com\migrator\App.java file with certain parameters.
- to generate SFCC XML Orders from Magento orders, the parameter is:
 > mode=getconvertxml
 
 - to generate Deck Commerce Orders from Magento orders, the parameter is:
 > mode=getconvertcsv
 
 ### NOTE: *ALL* operations which tap into the Magento API shall also download and save .json files in the *'saved_files'* directory by default.
 Unless you change the .env setting for 'BASE_SAVE_DIR' variable. Also, all the converted XML and/or CSV files shall share this same overall parent
 directory.
 
  - Also, if you just want the Magento order JSON saved as files without converting to SFCC or Deck, then you just need:
 > mode=get
 
  - To generate orders without hitting the Magento API, you can simply not have 'get' as the first part of the parameter.
 BUT, you will need to have properly formatted Magento order .json files saved. (batch Magento order format, not accessed for single orders):
 > mode=convertxml
 
 - to generate Deck Commerce Orders in this same manner:
 > mode=convertcsv
