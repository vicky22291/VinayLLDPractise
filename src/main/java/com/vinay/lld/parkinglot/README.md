# Design a Parking lot

## Key Requirements:

1. Support multiple vehicle types (Car, Motorcycle, Van, Truck)
2. Multiple parking spot types (Compact, Large, Handicapped, Electric)
3. Multiple floors with entry/exit points 
4. Pricing strategy and payment processing 
5. Real-time availability tracking

## What to Assess:

1. Design Patterns: Factory pattern for vehicle/spot creation, Strategy pattern for pricing, Observer pattern for display boards 
2. SOLID Principles: Single responsibility, Open/closed principle 
3. Concurrency: Thread-safe operations for spot allocation 
4. Scalability: How to handle multiple floors and high traffic 

## Expected Deliverables in 35 minutes:

1. Core classes: Vehicle, ParkingSpot, ParkingFloor, ParkingLot
2. At least 2-3 design patterns implemented
3. Basic functionality: park/unpark vehicles, find available spots
4. Discussion on scalability and edge cases