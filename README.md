

# 🖥️ Object Detection Platform (JavaFX + OpenCV)

A JavaFX-based Object Detection Platform that uses OpenCV to perform real-time object detection through a webcam.



## 🚀 Features
- Real-time video capture from the webcam
- OpenCV integration for live image processing
- JavaFX-powered smooth graphical user interface (GUI)
- Clean, modular, and scalable codebase
- Cross-platform compatibility (Windows, MacOS, Linux)



## 📦 Requirements
- Java 23
- JavaFX SDK 21.0.7
- OpenCV 4.1.1



## ⚙️ Installation and Setup

### 1. Clone the Repository
In bash
git clone https://github.com/shiven751/Object-detection-platform.git
cd Object-detection-platform


2. Install JavaFX
Download JavaFX SDK from GluonHQ.

Extract it and note the path to the lib/ directory.


3. Install OpenCV
Download OpenCV 4.1.1 from the OpenCV Official Site.

Extract it and copy:

opencv-4110.jar

opencv_java4110.dll

Place them inside the project directory (or update system PATH for the DLL).


4. Configure your IDE (VS Code / IntelliJ IDEA)
Add opencv-4110.jar and JavaFX libraries to your project/module.

Set the VM options to:

--module-path {path_to_javafx_lib} --add-modules javafx.controls,javafx.fxml

Ensure that the DLL is in your working directory or system path.





Project Structure

Object-detection-platform/
│
├── src/
│   └── application/
│       ├── Main.java
│       ├── style.css
│
├── resources/              # (Static files such as images if any)
├── opencv-4110.jar          # (OpenCV library)
├── opencv_java4110.dll      # (OpenCV native binding)
├── .gitignore
├── README.md
├── LICENSE
|-- javafx lib file
