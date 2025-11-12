import com.sun.jna.{Library, Native, Pointer}
import scala.util.Random

// ===== Minimal ncurses binding via JNA =====
object Curses {
  trait Lib extends Library {
    def initscr(): Pointer
    def endwin(): Int
    def cbreak(): Unit
    def noecho(): Unit
    def curs_set(visibility: Int): Int
    def keypad(win: Pointer, bf: Boolean): Unit
    def nodelay(win: Pointer, bf: Boolean): Unit
    def has_colors(): Boolean
    def start_color(): Int
    def use_default_colors(): Int
    def init_pair(pair: Short, f: Short, b: Short): Int
    def attron(attr: Int): Int
    def attroff(attr: Int): Int
    def mvaddch(y: Int, x: Int, ch: Int): Int
    def mvaddnstr(y: Int, x: Int, str: String, n: Int): Int
    def mvhline(y: Int, x: Int, ch: Int, n: Int): Int
    def getmaxy(win: Pointer): Int
    def getmaxx(win: Pointer): Int
    def newwin(nlines: Int, ncols: Int, beginY: Int, beginX: Int): Pointer
    def box(win: Pointer, verch: Int, horch: Int): Int
    def mvwaddnstr(win: Pointer, y: Int, x: Int, str: String, n: Int): Int
    def wrefresh(win: Pointer): Int
    def delwin(win: Pointer): Int
    def flushinp(): Unit
    def getch(): Int
    def wgetch(win: Pointer): Int
    def erase(): Int
    def refresh(): Int
    def napms(ms: Int): Int
  }

  val lib: Lib = Native.load("ncurses", classOf[Lib]).asInstanceOf[Lib]
}

// ===== Elements & Data =====
object Element extends Enumeration {
  type Element = Value
  val EMPTY,
      // powders
      SAND, GUNPOWDER, ASH, SNOW,
      // liquids
      WATER, SALTWATER, OIL, ETHANOL, ACID, LAVA, MERCURY,
      // solids / terrain
      STONE, GLASS, WALL, WOOD, PLANT, METAL, WIRE, ICE, COAL,
      DIRT, WET_DIRT, SEAWEED,
      // gases
      SMOKE, STEAM, GAS, TOXIC_GAS, HYDROGEN, CHLORINE,
      // actors / special
      FIRE, LIGHTNING, HUMAN, ZOMBIE = Value
}

object Category extends Enumeration {
  type Category = Value
  val POWDERS, LIQUIDS, SOLIDS, GASES, SPECIAL, CREDITS = Value
}

import Element._
import Category._

class Cell(var t: Element = EMPTY, var life: Int = 0)

case class MenuItem(t: Element, cat: Category, label: String, desc: String)

// ===== Main Sandbox =====
object Powder_sandbox {

  // curses globals
  var stdscr: Pointer = _

  // key / misc constants
  val ERR       = -1
  val KEY_LEFT  = 260
  val KEY_RIGHT = 261
  val KEY_UP    = 259
  val KEY_DOWN  = 258
  val KEY_ENTER = 10

  // color constants
  val COLOR_BLACK   = 0
  val COLOR_RED     = 1
  val COLOR_GREEN   = 2
  val COLOR_YELLOW  = 3
  val COLOR_BLUE    = 4
  val COLOR_MAGENTA = 5
  val COLOR_CYAN    = 6
  val COLOR_WHITE   = 7

  val A_REVERSE = 0x0200

  def COLOR_PAIR(n: Short): Int = (n.toInt << 8)

  // simulation globals
  var gWidth: Int = 0
  var gHeight: Int = 0
  var grid: Array[Array[Cell]] = Array.ofDim[Cell](0, 0)
  val rng: Random = new Random(System.nanoTime())

  // ===== Curses wrappers =====
  def initscr(): Unit = {
    stdscr = Curses.lib.initscr()
  }

  def endwin(): Int = Curses.lib.endwin()

  def cbreak(): Unit = Curses.lib.cbreak()

  def noecho(): Unit = Curses.lib.noecho()

  def curs_set(v: Int): Int = Curses.lib.curs_set(v)

  def keypad(win: Pointer, bf: Boolean): Unit = Curses.lib.keypad(win, bf)

  def nodelay(win: Pointer, bf: Boolean): Unit = Curses.lib.nodelay(win, bf)

  def has_colors(): Boolean = Curses.lib.has_colors()

  def start_color(): Int = Curses.lib.start_color()

  def use_default_colors(): Int = Curses.lib.use_default_colors()

  def init_pair(pair: Int, f: Int, b: Int): Int =
    Curses.lib.init_pair(pair.toShort, f.toShort, b.toShort)

  def attron(attr: Int): Int = Curses.lib.attron(attr)

  def attroff(attr: Int): Int = Curses.lib.attroff(attr)

  def mvaddch(y: Int, x: Int, ch: Char): Int =
    Curses.lib.mvaddch(y, x, ch.toInt)

  def mvaddch(y: Int, x: Int, ch: Int): Int =
    Curses.lib.mvaddch(y, x, ch)

  def mvaddnstr(y: Int, x: Int, s: String, n: Int): Int =
    Curses.lib.mvaddnstr(y, x, s, n)

  def mvhline(y: Int, x: Int, ch: Char, n: Int): Int =
    Curses.lib.mvhline(y, x, ch.toInt, n)

  // getmaxy/getmaxx wrapper instead of macro getmaxyx
  def getmaxyx(win: Pointer): (Int, Int) = {
    val y = Curses.lib.getmaxy(win)
    val x = Curses.lib.getmaxx(win)
    (y, x)
  }

  def newwin(h: Int, w: Int, y: Int, x: Int): Pointer =
    Curses.lib.newwin(h, w, y, x)

  def box(win: Pointer, verch: Char, horch: Char): Int =
    Curses.lib.box(win, verch.toInt, horch.toInt)

  def mvwaddnstr(win: Pointer, y: Int, x: Int, s: String, n: Int): Int =
    Curses.lib.mvwaddnstr(win, y, x, s, n)

  def wrefresh(win: Pointer): Int = Curses.lib.wrefresh(win)

  def delwin(win: Pointer): Int = Curses.lib.delwin(win)

  def flushinp(): Unit = Curses.lib.flushinp()

  def getch(): Int = Curses.lib.getch()

  def wgetch(win: Pointer): Int = Curses.lib.wgetch(win)

  def erase(): Int = Curses.lib.erase()

  def refresh(): Int = Curses.lib.refresh()

  def napms(ms: Int): Int = Curses.lib.napms(ms)

  // ===== Helpers / classification =====
  def in_bounds(x: Int, y: Int): Boolean =
    x >= 0 && x < gWidth && y >= 0 && y < gHeight

  def rint(a: Int, b: Int): Int =
    a + rng.nextInt(b - a + 1)

  def chance(p: Int): Boolean =
    (rng.nextInt(100) + 1) <= p

  def empty(c: Cell): Boolean = c.t == EMPTY

  def sandlike(e: Element): Boolean =
    e == SAND || e == GUNPOWDER || e == ASH || e == SNOW

  def liquid(e: Element): Boolean =
    e match {
      case WATER | SALTWATER | OIL | ETHANOL | ACID | LAVA | MERCURY => true
      case _ => false
    }

  def solid(e: Element): Boolean =
    e match {
      case STONE | GLASS | WALL | WOOD |
           PLANT | METAL | WIRE | ICE |
           COAL | DIRT | WET_DIRT | SEAWEED => true
      case _ => false
    }

  def gas(e: Element): Boolean =
    e match {
      case SMOKE | STEAM | GAS | TOXIC_GAS | HYDROGEN | CHLORINE => true
      case _ => false
    }

  def flammable(e: Element): Boolean =
    e == WOOD || e == PLANT || e == OIL ||
      e == ETHANOL || e == GUNPOWDER || e == COAL ||
      e == SEAWEED

  def conductor(e: Element): Boolean =
    e == METAL || e == WIRE || e == MERCURY || e == SALTWATER

  def dissolvable(e: Element): Boolean =
    e == SAND || e == STONE || e == GLASS ||
      e == WOOD || e == PLANT || e == METAL ||
      e == WIRE || e == ASH || e == COAL ||
      e == SEAWEED || e == DIRT || e == WET_DIRT

  def density(e: Element): Int =
    e match {
      case ETHANOL    => 85
      case OIL        => 90
      case GAS        => 1
      case HYDROGEN   => 1
      case STEAM      => 2
      case SMOKE      => 3
      case CHLORINE   => 5
      case WATER      => 100
      case SALTWATER  => 103
      case ACID       => 110
      case LAVA       => 160
      case MERCURY    => 200
      case _          => 999
    }

  def is_hazard(e: Element): Boolean =
    e == FIRE || e == LAVA || e == ACID ||
      e == TOXIC_GAS || e == CHLORINE || e == LIGHTNING

  def name_of(e: Element): String =
    e match {
      case EMPTY      => "Empty"
      case SAND       => "Sand"
      case GUNPOWDER  => "Gunpowder"
      case ASH        => "Ash"
      case SNOW       => "Snow"
      case WATER      => "Water"
      case SALTWATER  => "Salt Water"
      case OIL        => "Oil"
      case ETHANOL    => "Ethanol"
      case ACID       => "Acid"
      case LAVA       => "Lava"
      case MERCURY    => "Mercury"
      case STONE      => "Stone"
      case GLASS      => "Glass"
      case WALL       => "Wall"
      case WOOD       => "Wood"
      case PLANT      => "Plant"
      case METAL      => "Metal"
      case WIRE       => "Wire"
      case ICE        => "Ice"
      case COAL       => "Coal"
      case DIRT       => "Dirt"
      case WET_DIRT   => "Wet Dirt"
      case SEAWEED    => "Seaweed"
      case SMOKE      => "Smoke"
      case STEAM      => "Steam"
      case GAS        => "Gas"
      case TOXIC_GAS  => "Toxic Gas"
      case HYDROGEN   => "Hydrogen"
      case CHLORINE   => "Chlorine"
      case FIRE       => "Fire"
      case LIGHTNING  => "Lightning"
      case HUMAN      => "Human"
      case ZOMBIE     => "Zombie"
    }

  def color_of(e: Element): Short =
    e match {
      case EMPTY => 1.toShort
      // yellow-ish
      case SAND | GUNPOWDER | SNOW | DIRT =>
        2.toShort
      // cyan water-ish
      case WATER | SALTWATER | STEAM | ICE | ETHANOL =>
        3.toShort
      // white solids
      case STONE | GLASS | WALL | METAL | WIRE | COAL | WET_DIRT =>
        4.toShort
      // green stuff & humans
      case WOOD | PLANT | SEAWEED | HUMAN =>
        5.toShort
      // red danger
      case FIRE | LAVA | ZOMBIE =>
        6.toShort
      // magenta haze
      case SMOKE | ASH | GAS | HYDROGEN =>
        7.toShort
      // blue heavy liquids
      case OIL | MERCURY =>
        8.toShort
      // green/yellow chem/bolt
      case ACID | TOXIC_GAS | CHLORINE | LIGHTNING =>
        9.toShort
    }

  def glyph_of(e: Element): Char =
    e match {
      case EMPTY      => ' '
      case SAND       => '.'
      case GUNPOWDER  => '%'
      case ASH        => ';'
      case SNOW       => ','
      case WATER      => '~'
      case SALTWATER  => ':'
      case OIL        => 'o'
      case ETHANOL    => 'e'
      case ACID       => 'a'
      case LAVA       => 'L'
      case MERCURY    => 'm'
      case STONE      => '#'
      case GLASS      => '='
      case WALL       => '@'
      case WOOD       => 'w'
      case PLANT      => 'p'
      case SEAWEED    => 'v'
      case METAL      => 'M'
      case WIRE       => '-'
      case ICE        => 'I'
      case COAL       => 'c'
      case DIRT       => 'd'
      case WET_DIRT   => 'D'
      case SMOKE      => '^'
      case STEAM      => '"'
      case GAS        => '`'
      case TOXIC_GAS  => 'x'
      case HYDROGEN   => '\''
      case CHLORINE   => 'X'
      case FIRE       => '*'
      case LIGHTNING  => '|'
      case HUMAN      => 'Y'
      case ZOMBIE     => 'T'
    }

  // ===== Grid =====
  def init_grid(w: Int, h: Int): Unit = {
    gWidth = w
    gHeight = h
    grid = Array.ofDim[Cell](gHeight, gWidth)
    var y = 0
    while (y < gHeight) {
      var x = 0
      while (x < gWidth) {
        grid(y)(x) = new Cell()
        x += 1
      }
      y += 1
    }
  }

  def clear_grid(): Unit = {
    var y = 0
    while (y < gHeight) {
      var x = 0
      while (x < gWidth) {
        val c = grid(y)(x)
        c.t = EMPTY
        c.life = 0
        x += 1
      }
      y += 1
    }
  }

  def swapCells(a: Cell, b: Cell): Unit = {
    val tType = a.t
    val tLife = a.life
    a.t = b.t
    a.life = b.life
    b.t = tType
    b.life = tLife
  }

  // ===== Explosion & brush =====
  def explode(cx: Int, cy: Int, r: Int): Unit = {
    var dy = -r
    while (dy <= r) {
      var dx = -r
      while (dx <= r) {
        val x = cx + dx
        val y = cy + dy
        if (in_bounds(x, y) && dx * dx + dy * dy <= r * r) {
          val c = grid(y)(x)
          if (c.t != WALL &&
              c.t != STONE && c.t != GLASS &&
              c.t != METAL && c.t != WIRE &&
              c.t != ICE) {
            val roll = rint(1, 100)
            if (roll <= 50) {
              c.t = FIRE
              c.life = 15 + rint(0, 10)
            } else if (roll <= 80) {
              c.t = SMOKE
              c.life = 20
            } else {
              c.t = GAS
              c.life = 20
            }
          }
        }
        dx += 1
      }
      dy += 1
    }
  }

  def place_brush(cx: Int, cy: Int, rad: Int, e: Element): Unit = {
    if (e == LIGHTNING) {
      if (!in_bounds(cx, cy)) return
      val x = cx
      var y = cy

      // fall until just above the first non-empty, non-gas cell or bottom
      while (y + 1 < gHeight && {
        val below = grid(y + 1)(x)
        empty(below) || gas(below.t)
      }) {
        y += 1
      }

      var yy = cy
      while (yy <= y) {
        val c = grid(yy)(x)
        c.t = LIGHTNING
        c.life = 2
        yy += 1
      }

      if (y + 1 < gHeight) {
        val below = grid(y + 1)(x)
        if (below.t == WATER || below.t == SALTWATER) {
          below.life = math.max(below.life, 8)
        }
      }
      return
    }

    val r2 = rad * rad
    var dy = -rad
    while (dy <= rad) {
      var dx = -rad
      while (dx <= rad) {
        val x = cx + dx
        val y = cy + dy
        if (in_bounds(x, y) && dx * dx + dy * dy <= r2) {
          val c = grid(y)(x)
          c.t = e
          c.life = 0
          if (gas(e)) c.life = 25
          if (e == FIRE) c.life = 20
        }
        dx += 1
      }
      dy += 1
    }
  }

  // ===== Simulation =====
  def step_sim(): Unit = {
    if (gWidth <= 0 || gHeight <= 0) return
    val updated = Array.ofDim[Boolean](gHeight, gWidth)

    var y = gHeight - 1
    while (y >= 0) {
      var x = 0
      while (x < gWidth) {
        if (!updated(y)(x)) {
          val cell = grid(y)(x)
          val t = cell.t
          if (t == EMPTY || t == WALL) {
            updated(y)(x) = true
          } else {

            def swap_to(nx: Int, ny: Int): Unit = {
              swapCells(grid(ny)(nx), cell)
              updated(ny)(nx) = true
            }

            // --- powders ---
            if (sandlike(t)) {
              var moved = false

              if (in_bounds(x, y + 1)) {
                val below = grid(y + 1)(x)
                if (empty(below) || liquid(below.t)) {
                  swap_to(x, y + 1)
                  moved = true
                }
              }
              if (!moved) {
                val dir = if (rint(0, 1) == 1) 1 else -1
                var i = 0
                while (i < 2 && !moved) {
                  val nx = x + (if (i != 0) -dir else dir)
                  val ny = y + 1
                  if (in_bounds(nx, ny)) {
                    val d = grid(ny)(nx)
                    if (empty(d) || liquid(d.t)) {
                      swap_to(nx, ny)
                      moved = true
                    }
                  }
                  i += 1
                }
              }
              if (!moved) updated(y)(x) = true

              // SNOW melts near heat
              if (t == SNOW) {
                var dy = -1
                while (dy <= 1) {
                  var dx = -1
                  while (dx <= 1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (in_bounds(nx, ny)) {
                      val ne = grid(ny)(nx).t
                      if (ne == FIRE || ne == LAVA) {
                        cell.t = WATER
                        cell.life = 0
                      }
                    }
                    dx += 1
                  }
                  dy += 1
                }
              }

              // seaweed seed: sand under persistent water
              if (t == SAND) {
                if (in_bounds(x, y - 1) && grid(y - 1)(x).t == WATER) {
                  cell.life += 1
                  if (cell.life > 220) {
                    var nearbyWeed = false
                    var wy = -2
                    while (wy <= 2 && !nearbyWeed) {
                      var wx = -2
                      while (wx <= 2 && !nearbyWeed) {
                        val sx = x + wx
                        val sy = y + wy
                        if (in_bounds(sx, sy) && grid(sy)(sx).t == SEAWEED) {
                          nearbyWeed = true
                        }
                        wx += 1
                      }
                      wy += 1
                    }
                    if (!nearbyWeed && in_bounds(x, y - 1) &&
                      grid(y - 1)(x).t == WATER) {
                      grid(y - 1)(x).t = SEAWEED
                      grid(y - 1)(x).life = 0
                    }
                    cell.life = 0
                  }
                } else {
                  cell.life = 0
                }
              }

            }

            // --- liquids ---
            if (liquid(t)) {
              var moved = false

              if (in_bounds(x, y + 1)) {
                val b = grid(y + 1)(x)
                if (empty(b) || gas(b.t)) {
                  swap_to(x, y + 1)
                  moved = true
                } else if (liquid(b.t) && density(t) > density(b.t)) {
                  swap_to(x, y + 1)
                  moved = true
                }
              }

              if (!moved) {
                val order = Array(-1, 1)
                if (rint(0, 1) == 1) {
                  val tmp = order(0); order(0) = order(1); order(1) = tmp
                }
                var i = 0
                while (i < 2 && !moved) {
                  val nx = x + order(i)
                  if (in_bounds(nx, y)) {
                    val s = grid(y)(nx)
                    if (empty(s) || gas(s.t)) {
                      swap_to(nx, y)
                      moved = true
                    } else if (liquid(s.t) && density(t) > density(s.t) && chance(50)) {
                      swap_to(nx, y)
                      moved = true
                    }
                  }
                  i += 1
                }
              }

              if (!moved) updated(y)(x) = true

              var dy = -1
              while (dy <= 1) {
                var dx = -1
                while (dx <= 1) {
                  if (!(dx == 0 && dy == 0)) {
                    val nx = x + dx
                    val ny = y + dy
                    if (in_bounds(nx, ny)) {
                      val n = grid(ny)(nx)

                      if (t == WATER || t == SALTWATER) {
                        if (n.t == FIRE) {
                          n.t = SMOKE
                          n.life = 15
                        } else if (n.t == LAVA) {
                          n.t = STONE
                          n.life = 0
                          if (chance(50)) {
                            cell.t = STEAM
                            cell.life = 20
                          } else {
                            cell.t = STONE
                            cell.life = 0
                          }
                        }
                      }

                      if (t == OIL || t == ETHANOL) {
                        if (n.t == FIRE || n.t == LAVA) {
                          cell.t = FIRE
                          cell.life = 25
                        }
                      }

                      if (t == ACID) {
                        if (dissolvable(n.t)) {
                          if (chance(30)) {
                            n.t = TOXIC_GAS
                            n.life = 25
                          } else {
                            n.t = EMPTY
                            n.life = 0
                          }
                          if (chance(25)) {
                            cell.t = EMPTY
                            cell.life = 0
                          }
                        }
                        if (n.t == WATER && chance(30)) {
                          cell.t = SALTWATER
                          cell.life = 0
                          if (chance(30)) {
                            n.t = STEAM
                            n.life = 20
                          }
                        }
                      }

                      if (t == LAVA) {
                        if (flammable(n.t)) {
                          n.t = FIRE
                          n.life = 25
                        } else if (n.t == SAND || n.t == SNOW) {
                          n.t = GLASS
                          n.life = 0
                        } else if (n.t == WATER || n.t == SALTWATER) {
                          n.t = STONE
                          n.life = 0
                          if (chance(50)) {
                            cell.t = STEAM
                            cell.life = 20
                          } else {
                            cell.t = STONE
                            cell.life = 0
                          }
                        } else if (n.t == ICE) {
                          n.t = WATER
                          n.life = 0
                        }
                      }
                    }
                  }
                  dx += 1
                }
                dy += 1
              }

              if (t == LAVA) {
                cell.life += 1
                if (cell.life > 200) {
                  cell.t = STONE
                  cell.life = 0
                }
              }

              if (t == WATER || t == SALTWATER) {
                dy = -1
                while (dy <= 1) {
                  var dx = -1
                  while (dx <= 1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (in_bounds(nx, ny)) {
                      val n = grid(ny)(nx)
                      if (n.t == DIRT || n.t == WET_DIRT) {
                        n.t = WET_DIRT
                        n.life = 300
                      }
                    }
                    dx += 1
                  }
                  dy += 1
                }
              }

              if ((t == WATER || t == SALTWATER) && cell.life > 0) {
                val q = cell.life
                dy = -1
                while (dy <= 1) {
                  var dx = -1
                  while (dx <= 1) {
                    if (!(dx == 0 && dy == 0)) {
                      val nx = x + dx
                      val ny = y + dy
                      if (in_bounds(nx, ny)) {
                        val n = grid(ny)(nx)
                        if (n.t == WATER || n.t == SALTWATER) {
                          if (n.life < q - 1) n.life = q - 1
                        }
                        if (n.t == HUMAN || n.t == ZOMBIE) {
                          n.t = ASH
                          n.life = 0
                        }
                      }
                    }
                    dx += 1
                  }
                  dy += 1
                }
                cell.life -= 1
                if (cell.life < 0) cell.life = 0
              }

            }

            // --- gases ---
            if (gas(t)) {
              var moved = false

              val tries = if (t == HYDROGEN) 2 else 1
              var i = 0
              while (i < tries && !moved) {
                if (in_bounds(x, y - 1) && empty(grid(y - 1)(x))) {
                  swap_to(x, y - 1)
                  moved = true
                }
                i += 1
              }

              if (!moved) {
                val order = Array(-1, 1)
                if (rint(0, 1) == 1) {
                  val tmp = order(0); order(0) = order(1); order(1) = tmp
                }
                i = 0
                while (i < 2 && !moved) {
                  val nx = x + order(i)
                  val ny = y - (if (chance(50)) 1 else 0)
                  if (in_bounds(nx, ny) && empty(grid(ny)(nx))) {
                    swap_to(nx, ny)
                    moved = true
                  }
                  i += 1
                }
              }

              if (t == HYDROGEN || t == GAS) {
                var dy = -1
                while (dy <= 1) {
                  var dx = -1
                  while (dx <= 1) {
                    if (!(dx == 0 && dy == 0)) {
                      val nx = x + dx
                      val ny = y + dy
                      if (in_bounds(nx, ny)) {
                        val ne = grid(ny)(nx).t
                        if (ne == FIRE || ne == LAVA) {
                          if (t == HYDROGEN) {
                            explode(x, y, 4)
                          } else {
                            cell.t = FIRE
                            cell.life = 12
                          }
                        }
                      }
                    }
                    dx += 1
                  }
                  dy += 1
                }
              }

              if (t == CHLORINE) {
                var dy = -1
                while (dy <= 1) {
                  var dx = -1
                  while (dx <= 1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (in_bounds(nx, ny)) {
                      if (grid(ny)(nx).t == PLANT && chance(35)) {
                        grid(ny)(nx).t = TOXIC_GAS
                        grid(ny)(nx).life = 25
                      }
                    }
                    dx += 1
                  }
                  dy += 1
                }
              }

              cell.life -= 1
              if (cell.life <= 0) {
                if (t == STEAM && chance(15)) {
                  cell.t = WATER
                  cell.life = 0
                } else if (t == SMOKE && chance(8)) {
                  cell.t = ASH
                  cell.life = 0
                } else {
                  cell.t = EMPTY
                  cell.life = 0
                }
              } else {
                if (!moved) updated(y)(x) = true
              }

            }

            // --- fire ---
            if (t == FIRE) {
              if (in_bounds(x, y - 1) &&
                (empty(grid(y - 1)(x)) || gas(grid(y - 1)(x).t)) &&
                chance(50)) {
                swap_to(x, y - 1)
              }

              var dy = -1
              while (dy <= 1) {
                var dx = -1
                while (dx <= 1) {
                  if (!(dx == 0 && dy == 0)) {
                    val nx = x + dx
                    val ny = y + dy
                    if (in_bounds(nx, ny)) {
                      val n = grid(ny)(nx)
                      if (flammable(n.t) && chance(40)) {
                        if (n.t == GUNPOWDER) explode(nx, ny, 5)
                        else {
                          n.t = FIRE
                          n.life = 15 + rint(0, 10)
                        }
                      }
                      if (n.t == WATER || n.t == SALTWATER) {
                        cell.t = SMOKE
                        cell.life = 15
                      }
                      if (n.t == WIRE || n.t == METAL) {
                        if (chance(5)) n.life = math.max(n.life, 5)
                      }
                    }
                  }
                  dx += 1
                }
                dy += 1
              }

              cell.life -= 1
              if (cell.life <= 0) {
                cell.t = SMOKE
                cell.life = 15
              }
              updated(y)(x) = true

            }

            // --- lightning ---
            if (t == LIGHTNING) {
              var dy = -2
              while (dy <= 2) {
                var dx = -2
                while (dx <= 2) {
                  if (!(dx == 0 && dy == 0)) {
                    val nx = x + dx
                    val ny = y + dy
                    if (in_bounds(nx, ny)) {
                      val n = grid(ny)(nx)
                      val ne = n.t
                      if (ne == WIRE || ne == METAL) {
                        n.life = math.max(n.life, 12)
                      }
                      if (ne == WATER || ne == SALTWATER) {
                        n.life = math.max(n.life, 8)
                      }
                      if (flammable(ne)) {
                        if (ne == GUNPOWDER) explode(nx, ny, 6)
                        else {
                          n.t = FIRE
                          n.life = 20 + rint(0, 10)
                        }
                      }
                      if (ne == HYDROGEN || ne == GAS) {
                        explode(nx, ny, 4)
                      }
                    }
                  }
                  dx += 1
                }
                dy += 1
              }
              cell.life -= 1
              if (cell.life <= 0) {
                cell.t = EMPTY
                cell.life = 0
              }
              updated(y)(x) = true

            }

            // human/zombie walk helper
            def walk_try(tx: Int, ty: Int): Boolean = {
              if (!in_bounds(tx, ty)) return false
              val d = grid(ty)(tx)
              if (empty(d) || gas(d.t)) {
                swapCells(d, cell)
                true
              } else false
            }

            // --- HUMAN ---
            if (t == HUMAN) {
              var killed = false
              var dy = -1
              while (dy <= 1 && !killed) {
                var dx = -1
                while (dx <= 1 && !killed) {
                  val nx = x + dx
                  val ny = y + dy
                  if (in_bounds(nx, ny)) {
                    val ne = grid(ny)(nx).t
                    if (is_hazard(ne) ||
                      ((ne == WATER || ne == SALTWATER) && grid(ny)(nx).life > 0)) {
                      cell.t = ASH
                      cell.life = 0
                      killed = true
                    }
                  }
                  dx += 1
                }
                dy += 1
              }
              if (killed) {
                updated(y)(x) = true
              } else {
                cell.life += 1

                if (in_bounds(x, y + 1)) {
                  val b = grid(y + 1)(x).t
                  if (empty(grid(y + 1)(x)) || gas(b)) {
                    swap_to(x, y + 1)
                  }
                }

                var zx = 0; var zy = 0; var seen = false
                var ry = -6
                while (ry <= 6 && !seen) {
                  var rx = -6
                  while (rx <= 6 && !seen) {
                    val nx = x + rx
                    val ny = y + ry
                    if (in_bounds(nx, ny) && grid(ny)(nx).t == ZOMBIE) {
                      zx = nx; zy = ny; seen = true
                    }
                    rx += 1
                  }
                  ry += 1
                }

                dy = -1
                while (dy <= 1) {
                  var dx = -1
                  while (dx <= 1) {
                    if (!(dx == 0 && dy == 0)) {
                      val nx = x + dx
                      val ny = y + dy
                      if (in_bounds(nx, ny) && grid(ny)(nx).t == ZOMBIE && chance(35)) {
                        if (chance(60)) {
                          grid(ny)(nx).t = FIRE
                          grid(ny)(nx).life = 10 + rint(0, 10)
                        } else {
                          grid(ny)(nx).t = ASH
                          grid(ny)(nx).life = 0
                        }
                      }
                    }
                    dx += 1
                  }
                  dy += 1
                }

                var dir = if (rint(0, 1) == 1) 1 else -1
                if (seen) {
                  dir = if (zx < x) 1 else -1
                }

                if (!walk_try(x + dir, y)) {
                  if (in_bounds(x + dir, y - 1) &&
                    empty(grid(y - 1)(x + dir)) &&
                    empty(grid(y - 1)(x)) &&
                    chance(70)) {
                    swapCells(grid(y - 1)(x), cell)
                  } else {
                    walk_try(x + (if (rint(0, 1) == 1) 1 else -1), y)
                  }
                }

                updated(y)(x) = true
              }
            }

            // --- ZOMBIE ---
            if (t == ZOMBIE) {
              var dy = -1
              while (dy <= 1) {
                var dx = -1
                while (dx <= 1) {
                  val nx = x + dx
                  val ny = y + dy
                  if (in_bounds(nx, ny)) {
                    val ne = grid(ny)(nx).t
                    if (is_hazard(ne) ||
                      ((ne == WATER || ne == SALTWATER) && grid(ny)(nx).life > 0)) {
                      cell.t = FIRE
                      cell.life = 15
                    }
                  }
                  dx += 1
                }
                dy += 1
              }
              if (cell.t != ZOMBIE) {
                updated(y)(x) = true
              } else {
                cell.life += 1

                if (in_bounds(x, y + 1)) {
                  val b = grid(y + 1)(x).t
                  if (empty(grid(y + 1)(x)) || gas(b)) {
                    swap_to(x, y + 1)
                  }
                }

                var hx = 0; var hy = 0; var seen = false
                var ry = -6
                while (ry <= 6 && !seen) {
                  var rx = -6
                  while (rx <= 6 && !seen) {
                    val nx = x + rx
                    val ny = y + ry
                    if (in_bounds(nx, ny) && grid(ny)(nx).t == HUMAN) {
                      hx = nx; hy = ny; seen = true
                    }
                    rx += 1
                  }
                  ry += 1
                }

                dy = -1
                while (dy <= 1) {
                  var dx = -1
                  while (dx <= 1) {
                    if (!(dx == 0 && dy == 0)) {
                      val nx = x + dx
                      val ny = y + dy
                      if (in_bounds(nx, ny) && grid(ny)(nx).t == HUMAN) {
                        if (chance(70)) {
                          grid(ny)(nx).t = ZOMBIE
                          grid(ny)(nx).life = 0
                        } else {
                          grid(ny)(nx).t = FIRE
                          grid(ny)(nx).life = 10
                        }
                      }
                    }
                    dx += 1
                  }
                  dy += 1
                }

                var dir = if (seen) {
                  if (hx > x) 1 else -1
                } else if (rint(0, 1) == 1) 1 else -1

                if (!walk_try(x + dir, y)) {
                  if (in_bounds(x + dir, y - 1) &&
                    empty(grid(y - 1)(x + dir)) &&
                    empty(grid(y - 1)(x)) &&
                    chance(70)) {
                    swapCells(grid(y - 1)(x), cell)
                  } else {
                    walk_try(x + (if (rint(0, 1) == 1) 1 else -1), y)
                  }
                }

                updated(y)(x) = true
              }
            }

            // --- WET_DIRT drying ---
            if (t == WET_DIRT) {
              var nearWater = false
              var dy = -1
              while (dy <= 1 && !nearWater) {
                var dx = -1
                while (dx <= 1 && !nearWater) {
                  val nx = x + dx
                  val ny = y + dy
                  if (in_bounds(nx, ny)) {
                    val ne = grid(ny)(nx).t
                    if (ne == WATER || ne == SALTWATER) nearWater = true
                  }
                  dx += 1
                }
                dy += 1
              }
              if (!nearWater) {
                cell.life -= 1
                if (cell.life <= 0) {
                  cell.t = DIRT
                  cell.life = 0
                }
              }
              updated(y)(x) = true
            }

            // --- PLANT / SEAWEED ---
            if (t == PLANT || t == SEAWEED) {
              var dy = -1
              while (dy <= 1) {
                var dx = -1
                while (dx <= 1) {
                  if (!(dx == 0 && dy == 0)) {
                    val nx = x + dx
                    val ny = y + dy
                    if (in_bounds(nx, ny)) {
                      val nt = grid(ny)(nx).t
                      if (nt == FIRE || nt == LAVA) {
                        cell.t = FIRE
                        cell.life = 20
                      }
                    }
                  }
                  dx += 1
                }
                dy += 1
              }

              if (cell.t != PLANT && cell.t != SEAWEED) {
                updated(y)(x) = true
              } else {
                if (t == PLANT) {
                  val goodSoil = in_bounds(x, y + 1) && grid(y + 1)(x).t == WET_DIRT
                  if (goodSoil && chance(2)) {
                    val gx = x
                    val gy = y - 1
                    if (in_bounds(gx, gy) && empty(grid(gy)(gx))) {
                      grid(gy)(gx).t = PLANT
                      grid(gy)(gx).life = 0
                    }
                  }
                } else {
                  val underwater =
                    in_bounds(x, y - 1) &&
                      (grid(y - 1)(x).t == WATER || grid(y - 1)(x).t == SALTWATER)
                  val isTop = !in_bounds(x, y - 1) || grid(y - 1)(x).t != SEAWEED
                  if (underwater && isTop && chance(2)) {
                    val gy = y - 1
                    if (in_bounds(x, gy) &&
                      (grid(gy)(x).t == WATER || grid(gy)(x).t == SALTWATER)) {
                      grid(gy)(x).t = SEAWEED
                      grid(gy)(x).life = 0
                    }
                  }
                }

                updated(y)(x) = true
              }
            }

            // --- WOOD / COAL burn ---
            if (t == WOOD || t == COAL) {
              var dy = -1
              while (dy <= 1) {
                var dx = -1
                while (dx <= 1) {
                  if (!(dx == 0 && dy == 0)) {
                    val nx = x + dx
                    val ny = y + dy
                    if (in_bounds(nx, ny)) {
                      val nt = grid(ny)(nx).t
                      if (nt == FIRE || nt == LAVA) {
                        cell.t = FIRE
                        cell.life = if (t == COAL) 35 else 25
                      }
                    }
                  }
                  dx += 1
                }
                dy += 1
              }

              updated(y)(x) = true
            }

            // --- GUNPOWDER ---
            if (t == GUNPOWDER) {
              var dy = -1
              var exploded = false
              while (dy <= 1 && !exploded) {
                var dx = -1
                while (dx <= 1 && !exploded) {
                  if (!(dx == 0 && dy == 0)) {
                    val nx = x + dx
                    val ny = y + dy
                    if (in_bounds(nx, ny)) {
                      val ne = grid(ny)(nx).t
                      if (ne == FIRE || ne == LAVA) {
                        explode(x, y, 5)
                        exploded = true
                      }
                    }
                  }
                  dx += 1
                }
                dy += 1
              }
              updated(y)(x) = true
            }

            // --- WIRE / METAL conduction ---
            if (t == WIRE || t == METAL) {
              if (cell.life > 0) {
                val q = cell.life
                var dy = -1
                while (dy <= 1) {
                  var dx = -1
                  while (dx <= 1) {
                    if (!(dx == 0 && dy == 0)) {
                      val nx = x + dx
                      val ny = y + dy
                      if (in_bounds(nx, ny)) {
                        val n = grid(ny)(nx)
                        if (n.t == WIRE || n.t == METAL) {
                          if (n.life < q - 1) n.life = q - 1
                        }
                        if (n.t == WATER || n.t == SALTWATER) {
                          if (n.life < q - 1) n.life = q - 1
                        }
                        if (flammable(n.t) && chance(15)) {
                          if (n.t == GUNPOWDER) explode(nx, ny, 5)
                          else {
                            n.t = FIRE
                            n.life = 15 + rint(0, 10)
                          }
                        }
                        if (n.t == HYDROGEN || n.t == GAS) {
                          if (chance(35)) explode(nx, ny, 4)
                        }
                      }
                    }
                    dx += 1
                  }
                  dy += 1
                }
                cell.life -= 1
                if (cell.life < 0) cell.life = 0
              }
              updated(y)(x) = true
            }

            // --- ICE ---
            if (t == ICE) {
              var dy = -1
              while (dy <= 1) {
                var dx = -1
                while (dx <= 1) {
                  val nx = x + dx
                  val ny = y + dy
                  if (in_bounds(nx, ny)) {
                    val ne = grid(ny)(nx).t
                    if (ne == FIRE || ne == LAVA || ne == STEAM) {
                      if (chance(25)) {
                        cell.t = WATER
                        cell.life = 0
                      }
                    }
                  }
                  dx += 1
                }
                dy += 1
              }
              updated(y)(x) = true
            }

            // default static for anything else
            updated(y)(x) = true
          }
        }
        x += 1
      }
      y -= 1
    }
  }

  // ===== Drawing =====
  def draw_grid(cx: Int, cy: Int, cur: Element, paused: Boolean, brush: Int): Unit = {
    var y = 0
    while (y < gHeight) {
      var x = 0
      while (x < gWidth) {
        val c = grid(y)(x)
        var ch = glyph_of(c.t)

        if (c.t == HUMAN)  ch = if ((c.life / 6) % 2 != 0) 'y' else 'Y'
        if (c.t == ZOMBIE) ch = if ((c.life / 6) % 2 != 0) 't' else 'T'
        if (c.t == LIGHTNING) ch = '|'

        var col = color_of(c.t)
        if ((c.t == WATER || c.t == SALTWATER) && c.life > 0) {
          col = 9.toShort
        }

        if (has_colors()) attron(COLOR_PAIR(col))
        mvaddch(y, x, ch)
        if (has_colors()) attroff(COLOR_PAIR(col))

        x += 1
      }
      y += 1
    }

    if (in_bounds(cx, cy)) mvaddch(cy, cx, '+')

    val (maxy, maxx) = getmaxyx(stdscr)

    if (gHeight < maxy) mvhline(gHeight, 0, '-', maxx)

    var status =
      "Move: Arrows/WASD | Space: draw | E: erase | +/-: brush | C/X: clear | " +
        "P: pause | M/Tab: elements | Q: quit"
    if (status.length > maxx) status = status.substring(0, maxx)
    if (gHeight + 1 < maxy) mvaddnstr(gHeight + 1, 0, status, maxx)

    var info = "Current: " + name_of(cur) + " | Brush r=" + brush +
      (if (paused) " [PAUSED]" else "")
    if (info.length > maxx) info = info.substring(0, maxx)
    if (gHeight + 2 < maxy) mvaddnstr(gHeight + 2, 0, info, maxx)
  }

  // ===== Element Browser & Credits =====
  val MENU: Seq[MenuItem] = Seq(
    // Powders
    MenuItem(SAND,      POWDERS, "Sand",      "Classic falling grains."),
    MenuItem(GUNPOWDER, POWDERS, "Gunpowder", "Explodes when ignited."),
    MenuItem(ASH,       POWDERS, "Ash",       "Burnt residue."),
    MenuItem(SNOW,      POWDERS, "Snow",      "Melts near heat."),

    // Liquids
    MenuItem(WATER,     LIQUIDS, "Water",     "Flows, cools, extinguishes."),
    MenuItem(SALTWATER, LIQUIDS, "Salt Water","Conductive water."),
    MenuItem(OIL,       LIQUIDS, "Oil",       "Light, flammable."),
    MenuItem(ETHANOL,   LIQUIDS, "Ethanol",   "Very flammable."),
    MenuItem(ACID,      LIQUIDS, "Acid",      "Dissolves many materials."),
    MenuItem(LAVA,      LIQUIDS, "Lava",      "Hot molten rock."),
    MenuItem(MERCURY,   LIQUIDS, "Mercury",   "Heavy liquid metal."),

    // Solids
    MenuItem(STONE,     SOLIDS,  "Stone",     "Heavy solid block."),
    MenuItem(GLASS,     SOLIDS,  "Glass",     "From sand + lava."),
    MenuItem(WALL,      SOLIDS,  "Wall",      "Indestructible barrier."),
    MenuItem(WOOD,      SOLIDS,  "Wood",      "Flammable solid."),
    MenuItem(PLANT,     SOLIDS,  "Plant",     "Grows on wet dirt."),
    MenuItem(SEAWEED,   SOLIDS,  "Seaweed",   "Grows in water over sand."),
    MenuItem(METAL,     SOLIDS,  "Metal",     "Conductive solid."),
    MenuItem(WIRE,      SOLIDS,  "Wire",      "Conductive path."),
    MenuItem(ICE,       SOLIDS,  "Ice",       "Melts into water."),
    MenuItem(COAL,      SOLIDS,  "Coal",      "Burns longer."),
    MenuItem(DIRT,      SOLIDS,  "Dirt",      "Gets wet; grows plants."),
    MenuItem(WET_DIRT,  SOLIDS,  "Wet Dirt",  "Dries over time."),

    // Gases
    MenuItem(SMOKE,     GASES,   "Smoke",     "Rises; may fall as ash."),
    MenuItem(STEAM,     GASES,   "Steam",     "Condenses to water."),
    MenuItem(GAS,       GASES,   "Gas",       "Neutral rising gas."),
    MenuItem(TOXIC_GAS, GASES,   "Toxic Gas", "Nasty chemical cloud."),
    MenuItem(HYDROGEN,  GASES,   "Hydrogen",  "Very light, explosive."),
    MenuItem(CHLORINE,  GASES,   "Chlorine",  "Harms plants."),

    // Special
    MenuItem(FIRE,      SPECIAL, "Fire",      "Burns & flickers upward."),
    MenuItem(LIGHTNING, SPECIAL, "Lightning", "Yellow electrical bolt."),
    MenuItem(HUMAN,     SPECIAL, "Human",     "Avoids zombie, fights back."),
    MenuItem(ZOMBIE,    SPECIAL, "Zombie",    "Chases and infects humans."),
    MenuItem(EMPTY,     SPECIAL, "Eraser",    "Place empty space."),

    // Credits
    MenuItem(EMPTY,     CREDITS, "Credits",   "Show credits & license.")
  )

  def cat_name(c: Category): String =
    c match {
      case POWDERS => "Powders"
      case LIQUIDS => "Liquids"
      case SOLIDS  => "Solids"
      case GASES   => "Gases"
      case SPECIAL => "Special"
      case CREDITS => "Credits"
    }

  def show_credits_overlay(): Unit = {
    val (maxy, maxx) = getmaxyx(stdscr)
    if (maxx < 40 || maxy < 12) return

    val w = math.min(maxx - 4, 70)
    val h = math.min(maxy - 4, 15)
    val ty = (maxy - h) / 2
    val lx = (maxx - w) / 2

    val win = newwin(h, w, ty, lx)
    if (win == Pointer.NULL) return

    box(win, 0.toChar, 0.toChar)
    val title = " Credits "
    mvwaddnstr(win, 0, (w - title.length) / 2, title, w - 2)

    val lines = Array(
      "Terminal Powder Toy-like Sandbox",
      "Author: Robert",
      "GitHub: https://github.com/RobertFlexx",
      "Language: Scala + ncurses + JNA",
      "",
      "BSD 3-Clause License (snippet):",
      "Redistribution and use in source and binary forms,",
      "with or without modification, are permitted provided",
      "that the following conditions are met:",
      "1) Source redistributions retain this notice & disclaimer.",
      "2) Binary redistributions reproduce this notice & disclaimer.",
      "3) Names of contributors can't be used to endorse products",
      "   derived from this software without permission.",
      "",
      "Press any key to return."
    )

    var y = 2
    var i = 0
    while (i < lines.length && y < h - 1) {
      mvwaddnstr(win, y, 2, lines(i), w - 4)
      y += 1
      i += 1
    }

    wrefresh(win)
    flushinp()
    wgetch(win)
    delwin(win)
  }

  def element_menu(current: Element): Element = {
    val tabs = Array(POWDERS, LIQUIDS, SOLIDS, GASES, SPECIAL, CREDITS)
    val NT = tabs.length

    var curTab: Category = POWDERS
    MENU.foreach { it =>
      if (it.t == current) curTab = it.cat
    }

    var tabIdx = 0
    var i = 0
    while (i < NT) {
      if (tabs(i) == curTab) tabIdx = i
      i += 1
    }

    var sel = 0
    var done = false
    var result = current

    while (!done) {
      val (maxy, maxx) = getmaxyx(stdscr)

      val idx: Seq[Int] = MENU.indices.filter(i => MENU(i).cat == tabs(tabIdx))

      if (sel < 0) sel = 0
      if (idx.nonEmpty && sel >= idx.size) sel = idx.size - 1

      var boxW = math.max(44, maxx - 6)
      var boxH = math.max(14, maxy - 6)
      boxW = math.min(boxW, maxx)
      boxH = math.min(boxH, maxy)
      val lx = (maxx - boxW) / 2
      val ty = (maxy - boxH) / 2
      val rx = lx + boxW - 1
      val by = ty + boxH - 1

      erase()
      mvaddch(ty, lx, '+')
      mvaddch(ty, rx, '+')
      mvaddch(by, lx, '+')
      mvaddch(by, rx, '+')

      var x = lx + 1
      while (x < rx) {
        mvaddch(ty, x, '-')
        mvaddch(by, x, '-')
        x += 1
      }

      var y = ty + 1
      while (y < by) {
        mvaddch(y, lx, '|')
        mvaddch(y, rx, '|')
        y += 1
      }

      val title = " Element Browser "
      mvaddnstr(ty, lx + (boxW - title.length) / 2, title, boxW - 2)

      val tabsY = ty + 1
      var cx = lx + 2
      i = 0
      while (i < NT) {
        val tab = " " + cat_name(tabs(i)) + " "
        if (cx + tab.length >= rx) {
          // stop rendering tabs if no space
        } else {
          if (i == tabIdx) attron(A_REVERSE)
          mvaddnstr(tabsY, cx, tab, rx - cx - 1)
          if (i == tabIdx) attroff(A_REVERSE)
          cx += tab.length + 1
        }
        i += 1
      }

      y = ty + 3
      val maxListY = by - 3
      var ii = 0
      while (ii < idx.size && y <= maxListY) {
        val it = MENU(idx(ii))
        var line = " " + it.label + " - " + it.desc
        if (line.length > boxW - 4) line = line.substring(0, boxW - 4)
        if (ii == sel) attron(A_REVERSE)
        mvaddnstr(y, lx + 2, line, boxW - 4)
        if (ii == sel) attroff(A_REVERSE)
        y += 1
        ii += 1
      }

      val hint = "Left/Right: tabs | Up/Down: select | Enter: choose | ESC: back"
      mvaddnstr(by - 1, lx + 2, hint, boxW - 4)
      refresh()

      val ch = getch()
      ch match {
        case c if c == KEY_LEFT =>
          tabIdx = (tabIdx + NT - 1) % NT
          sel = 0
        case c if c == KEY_RIGHT =>
          tabIdx = (tabIdx + 1) % NT
          sel = 0
        case c if c == KEY_UP =>
          if (idx.nonEmpty) sel = (sel + idx.size - 1) % idx.size
        case c if c == KEY_DOWN =>
          if (idx.nonEmpty) sel = (sel + 1) % idx.size
        case c if c == '\n'.toInt || c == '\r'.toInt || c == KEY_ENTER =>
          if (idx.nonEmpty) {
            val it = MENU(idx(sel))
            if (it.cat == CREDITS) {
              show_credits_overlay()
            } else {
              result = it.t
              done = true
            }
          } else {
            done = true
          }
        case 27 => // ESC
          done = true
        case _ =>
      }
    }

    result
  }

  // ===== Main =====
  def main(args: Array[String]): Unit = {
    initscr()
    cbreak()
    noecho()
    curs_set(0)
    keypad(stdscr, true)
    nodelay(stdscr, true)

    val (termH0, termW0) = getmaxyx(stdscr)
    val simH0 = math.max(1, termH0 - 3)
    init_grid(termW0, simH0)

    if (has_colors()) {
      start_color()
      use_default_colors()
      init_pair(1, COLOR_BLACK,   -1)
      init_pair(2, COLOR_YELLOW,  -1)
      init_pair(3, COLOR_CYAN,    -1)
      init_pair(4, COLOR_WHITE,   -1)
      init_pair(5, COLOR_GREEN,   -1)
      init_pair(6, COLOR_RED,     -1)
      init_pair(7, COLOR_MAGENTA, -1)
      init_pair(8, COLOR_BLUE,    -1)
      init_pair(9, COLOR_YELLOW,  -1)
    }

    var cx = if (gWidth > 0) gWidth / 2 else 0
    var cy = if (gHeight > 0) gHeight / 2 else 0
    var brush = 1
    var current: Element = SAND
    var running = true
    var paused = false

    while (running) {
      val (nh, nw) = getmaxyx(stdscr)
      val nSimH = math.max(1, nh - 3)
      if (nw != gWidth || nSimH != gHeight) {
        init_grid(nw, nSimH)
        if (cx < 0) cx = 0
        if (cx > gWidth - 1) cx = gWidth - 1
        if (cy < 0) cy = 0
        if (cy > gHeight - 1) cy = gHeight - 1
      }

      var keepReading = true
      while (keepReading) {
        val ch = getch()
        if (ch == ERR) {
          keepReading = false
        } else {
          ch match {
            case c if c == 'q'.toInt || c == 'Q'.toInt =>
              running = false

            case c if c == KEY_LEFT || c == 'a'.toInt || c == 'A'.toInt =>
              cx = math.max(0, cx - 1)

            case c if c == KEY_RIGHT || c == 'd'.toInt || c == 'D'.toInt =>
              cx = math.min(gWidth - 1, cx + 1)

            case c if c == KEY_UP || c == 'w'.toInt =>
              cy = math.max(0, cy - 1)

            case c if c == KEY_DOWN || c == 's'.toInt || c == 'S'.toInt =>
              cy = math.min(gHeight - 1, cy + 1)

            case c if c == ' '.toInt =>
              place_brush(cx, cy, brush, current)

            case c if c == 'e'.toInt || c == 'E'.toInt =>
              place_brush(cx, cy, brush, EMPTY)

            case c if c == '+'.toInt || c == '='.toInt =>
              if (brush < 8) brush += 1

            case c if c == '-'.toInt || c == '_'.toInt =>
              if (brush > 1) brush -= 1

            case c if c == 'c'.toInt || c == 'C'.toInt ||
                       c == 'x'.toInt || c == 'X'.toInt =>
              clear_grid()

            case c if c == 'p'.toInt || c == 'P'.toInt =>
              paused = !paused

            case c if c == 'm'.toInt || c == 'M'.toInt || c == '\t'.toInt =>
              flushinp()
              nodelay(stdscr, false)
              current = element_menu(current)
              nodelay(stdscr, true)

            case c if c == '1'.toInt =>
              current = SAND
            case c if c == '2'.toInt =>
              current = WATER
            case c if c == '3'.toInt =>
              current = STONE
            case c if c == '4'.toInt =>
              current = WOOD
            case c if c == '5'.toInt =>
              current = FIRE
            case c if c == '6'.toInt =>
              current = OIL
            case c if c == '7'.toInt =>
              current = LAVA
            case c if c == '8'.toInt =>
              current = PLANT
            case c if c == '9'.toInt =>
              current = GUNPOWDER
            case c if c == '0'.toInt =>
              current = ACID
            case c if c == 'W'.toInt =>
              current = WALL
            case c if c == 'L'.toInt =>
              current = LIGHTNING
            case c if c == 'H'.toInt || c == 'h'.toInt =>
              current = HUMAN
            case c if c == 'Z'.toInt =>
              current = ZOMBIE
            case c if c == 'D'.toInt =>
              current = DIRT

            case _ =>
          }
        }
      }

      if (!paused) step_sim()

      erase()
      draw_grid(cx, cy, current, paused, brush)
      refresh()
      napms(16)
    }

    endwin()
  }
}
