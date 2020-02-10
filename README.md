game-ai-projects
================

Various projects related to game AI.

## Robocode

A simple but decently performing bot for Robocode. This uses simple AI technique, such as minimax movement (minimum risk) as well as a statistical targeting system based on "Waves" (essentially a basic learning algorithm that adjusts its firing behavior based on historical data).

## Wargus

Simple AI that consists of several manager classes that work together to help the computer player to accomplish its goal. The build manager consists of predefined build orders (based on user experience) as well as algorithms for efficient building placement. In addition, the build manager automatically repairs damaged buildings.

The unit manager is mostly reactionary; i.e it is based on responding to actions made by the opponent. After a large enough army is built up, the computer player will also attack the enemy.

The resource manager simply tells workers to harvest the nearest resources when needed.
