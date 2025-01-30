# 📄 Invoice Service API

🚀 A Spring Boot application for handling invoice conversion from XML files to HTML format.  
The service allows users to upload an XML invoice file, which gets processed and converted into an HTML format.  
The generated HTML can then be downloaded.

---

## ✨ Features

✅ **Convert XML to HTML**: Upload XML invoice files and receive an HTML conversion.  
✅ **Download HTML Invoice**: Download the generated HTML invoice by specifying its `invoiceId`.  
✅ **Cross-Origin Resource Sharing (CORS)**: The API supports cross-origin requests.  

---

## ⚙️ Prerequisites

Before deploying this project, ensure you have the following installed on your **Windows** system:

### 🔹 1. Java Development Kit (JDK)  
🔗 **Windows x64 Installer:**  
[Download JDK](https://www.oracle.com/java/technologies/javase/jdk23-archive-downloads.html)  

### 🔹 2. Maven (For building the project)

#### 📥 Step 1: Download Maven  
Download the **apache-maven-3.9.9-bin.zip** file and extract it to:  
C:\Program Files\Maven\apache-maven-3.8.4

# 🛠 Setting Up Maven and Running the Project

## ⚙️ Step 2: Add `MAVEN_HOME` System Variable  

1️⃣ Open the **Start menu** and search for *environment variables*.  
2️⃣ Click **Edit the system environment variables**.  
3️⃣ Under the **Advanced** tab, click **Environment Variables**.  
4️⃣ Click **New** (under *System variables*) and enter:  
   - **Variable Name:** `MAVEN_HOME`  
   - **Variable Value:** Path to the Maven directory (e.g., `C:\Program Files\Maven\apache-maven-3.8.4`)  
5️⃣ Click **OK** to save.

---

## 🔗 Step 3: Add `MAVEN_HOME` Directory to `PATH`  

1️⃣ In the *Environment Variables* window, select **Path** (under *System variables*) and click **Edit**.  
2️⃣ Click **New** and enter:  
   ```plaintext
   %MAVEN_HOME%\bin


💡 **Note:**  
Not adding the path to the Maven home directory to the `PATH` variable may cause the following error when using the `mvn` command:  
```plaintext
'mvn' is not recognized as an internal or external command, operable program, or batch file.



Step 4: Verify Maven Installation
mvn -version

## Setting up the Project
Configure the Output Directory
in application.properties:
invoice.output-dir=C:/path/to/output/directory

Make sure that the output directory exists or create it manually.



## Build The Project
Once all dependencies are installed and configured, you can build the project using Maven.
mvn clean install

## Run the Application
mvn spring-boot:run


