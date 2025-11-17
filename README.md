# INTERACTIVE-IMAGE-SELECTION-APP

A Java Swing application for interactive and edge-aware image segmentation.

This project implements an image-selection tool that allows users to outline and extract objects from images using both point-to-point polygon selection and an “intelligent scissors” edge-tracing algorithm.

# Features :

Interactive GUI (Java Swing): Open images, click to add control points, drag to adjust vertices, undo/reset selections, and save cropped outputs.

Two Selection Modes:

Point-to-Point: Straight-line polygonal selection. 

Intelligent Scissors: Dijkstra-based shortest-path tracing that follows image edges.

Graph-Based Edge Tracing:

Custom heap-based priority queue

Dijkstra’s algorithm over a pixel-graph

Adjustable edge weights based on color/brightness gradients

Responsive Design: Background computation via SwingWorker keeps the UI responsive, with progress updates.


# How to Run:

Clone the repository

Open the project in IntelliJ or any Java 17+ environment

Run the main class (SelectorApp.java)

Use the GUI to open an image and start selecting. Have fun!

# Screenshots:

Point-to-Point Labeling:

<img width="728" height="602" alt="image" src="https://github.com/user-attachments/assets/e9dfc1f8-f924-4ae0-a27d-13228a4a038a" />

File -> save produces saved output:

<img width="118" height="389" alt="fin" src="https://github.com/user-attachments/assets/24c81f8a-0f64-4489-b374-311e65ae61d1" />

Smart Scissors Labelling:

<img width="723" height="577" alt="image" src="https://github.com/user-attachments/assets/62e51696-89c5-4fdb-b403-bee0569a1ceb" />

Output:

<img width="112" height="389" alt="fin3" src="https://github.com/user-attachments/assets/ff32e286-e62f-4172-8162-f061be82568d" />





