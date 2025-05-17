# SOS Emergency App

## Overview

The **SOS Emergency App** is designed to help users in emergency situations by sending SOS messages to pre-configured contacts along with their real-time location. It also helps users find nearby hospitals based on their current location. The app is built using Kotlin and Android Studio, integrating Firebase for user authentication and Firestore for storing contact data.

## Features

- **SOS Message**: Send emergency messages to a list of contacts with your real-time location and predefined emergency types (e.g., Accident, Fire, Violence).
- **Find Nearby Hospitals**: Automatically fetches nearby hospitals using the Overpass API based on the user's location and includes them in the SOS message.
- **Login/Signup**: User authentication using Firebase.
- **Contact Management**: Add and manage a list of emergency contacts who will receive the SOS messages.
- **Firestore Integration**: Stores the user's contacts in Firebase Firestore for secure access across devices.

## Tech Stack

- **Android**: Kotlin, XML, Android Studio
- **Firebase**: Firebase Authentication, Firestore
- **API Integration**: Overpass API for fetching nearby hospitals
- **SMS**: Send SMS messages to emergency contacts

## Prerequisites

- Android Studio
- Kotlin 1.5+
- Firebase Project (Authentication and Firestore enabled)
- Retrofit for network requests (Overpass API)


## Clone the repository:
   ```bash
   git clone https://github.com/Ajay-patidar0/SOS-app
   
