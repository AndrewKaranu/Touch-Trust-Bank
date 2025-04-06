# Touch-Trust-Bank

An inclusive banking application designed to provide a seamless online banking experience for visually impaired users.

## Overview

Touch-Trust-Bank aims to bridge the gap in accessibility within the online banking sector. By incorporating features such as:

*   **Screen Reader Compatibility:** Ensuring all elements are properly labeled and navigable with screen readers.
*   **Voice Recognition and Text-to-Speech:** Enabling hands-free interaction and clear audio feedback.
*   **High Contrast Themes:** Offering customizable visual settings for enhanced readability.
*   **Keyboard Navigation:** Providing full functionality without the need for a mouse.

This application empowers visually impaired individuals to manage their finances independently and securely.

## Features

*   Account Management: View balances, transaction history, and account details.
*   Bill Payment: Schedule and pay bills with ease.
*   Funds Transfer: Transfer money between accounts or to external recipients.
*   Security Features: Robust security measures to protect user data and prevent fraud.
*   Customizable Accessibility Settings: Tailor the application to individual visual needs.

## Technologies Used

*   JavaFX: For building the user interface.
*   SQLite: For local data storage.
*   jBCrypt: For password hashing.
*   OpenCV: For image processing and facial recognition features.
*   CMU Sphinx4: For speech recognition capabilities.
*   Google Cloud Text-to-Speech: For generating spoken output.

## Dependencies

The project uses the following dependencies, managed by Maven:

*   JavaFX (Controls, FXML, Web, Swing)
*   ControlsFX
*   FormsFX
*   Ikonli
*   BootstrapFX
*   JUnit
*   FontAwesomeFX
*   SQLite JDBC
*   jBCrypt
*   JavaCV Platform
*   flandmark
*   jlayer
*   Google Cloud Text-to-Speech

(See `pom.xml` for specific versions)

## Getting Started

1.  Clone the repository: `git clone [repository URL]`
2.  Build the project using Maven: `mvn clean install`
3.  Run the application: `mvn javafx:run`