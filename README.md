# Powder Sandbox (Scala JVM Edition)

The **Scala Edition** of Powder Sandbox brings the terminal-based falling-sand simulator into the expressive, type-safe world of Scala. Built on the JVM, it shares the same simulation logic and TUI design as the Kotlin and Groovy editions, but leverages **Scala’s blend of functional and object-oriented programming** for richer abstractions and more expressive rule systems.

This version appeals to those who love precision, structure, and power — every behavior can be defined through elegant pattern matching and functional composition.

---

## Features

* Written in **Scala** (JVM-based)
* Fully cross-platform (Linux, macOS, Windows)
* Dynamic falling-sand simulation (sand, water, lava, smoke, gases, etc.)
* Electrical behavior with wire, lightning, and conductive elements
* AI-controlled entities (humans and zombies)
* Rich colorized terminal interface with interactive menus
* Immutable data logic with Scala collections
* Strong static typing and safe functional patterns
* Modular design for new elements and simulation extensions

---

## Requirements

* Java 17+ or OpenJDK equivalent
* Scala 3.x or 2.13+

Install on Debian/Ubuntu:

```bash
sudo apt install openjdk-17-jdk scala
```

---

## Building and Running

Clone and enter the project:

```bash
git clone https://github.com/RobertFlexx/Powder-Sandbox-Scala-Edition
cd Powder-Sandbox-Scala-Edition
```

Compile and run with `scalac`:

```bash
scalac -cp /usr/share/java/jna.jar PowderSandbox.scala
scala -cp .:/usr/share/java/jna.jar PowderSandbox
```

Or if using SBT:

```bash
sbt run
```

---

## Controls

| Key               | Action                 |
| ----------------- | ---------------------- |
| Arrow keys / WASD | Move cursor            |
| Space             | Place current element  |
| E                 | Erase with empty space |
| + / -             | Adjust brush size      |
| M / Tab           | Open element menu      |
| P                 | Pause simulation       |
| C / X             | Clear screen           |
| Q                 | Quit simulation        |

---

## Comparison: Scala vs [Kotlin Edition](https://github.com/RobertFlexx/Powder-Sandbox-Kotlin-Edition) vs [Groovy Edition](https://github.com/RobertFlexx/Powder-Sandbox-Groovy-Edition)

| Aspect            | Scala Edition (Functional/OO Hybrid)                    | [Kotlin Edition](https://github.com/RobertFlexx/Powder-Sandbox-Kotlin-Edition) | [Groovy Edition](https://github.com/RobertFlexx/Powder-Sandbox-Groovy-Edition) |
| ----------------- | ------------------------------------------------------- | ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------ |
| Language Style    | Functional + OO blend with advanced type system         | Modern, pragmatic, concise syntax                                              | Dynamic and scripting-oriented                                                 |
| Type System       | Extremely strong static typing, sealed traits, and ADTs | Strong static typing with null safety                                          | Dynamic typing (runtime-checked)                                               |
| Performance       | High JVM performance (optimized JIT)                    | High JVM performance (JIT optimized)                                           | Moderate performance (dynamic runtime)                                         |
| Readability       | Expressive but verbose for newcomers                    | Clean and balanced                                                             | Simple and relaxed syntax                                                      |
| Safety            | Excellent via immutability and compile-time checking    | Excellent with null safety and Kotlin contracts                                | Looser runtime safety                                                          |
| Interoperability  | Great Java interop, but slightly heavier setup          | Excellent Java interop                                                         | Perfect for scripting Java APIs                                                |
| Abstraction Power | Very high — supports DSL-like constructs                | Moderate, simpler and more direct                                              | Low, but extremely easy to modify                                              |
| Ideal Use Case    | Large or academic JVM simulations                       | Balanced general-purpose JVM development                                       | Rapid prototyping and rule experiments                                         |

All three editions share the **same simulation logic and controls** — their differences come down to how they express that logic:

* **Scala** — for expressive, functional design and code elegance.
* **Kotlin** — for clean pragmatism and modern readability.
* **Groovy** — for quick iteration and live experimentation.

---

## License

Released under the BSD 3-Clause License.

---

## Author

**Robert (@RobertFlexx)**
Creator of FerriteOS, experimental shells, editors, and high-performance terminal simulations.

GitHub: [https://github.com/RobertFlexx](https://github.com/RobertFlexx)
