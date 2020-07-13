/*
    Copyright (C) 2020  Jannes Althoff

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>
*/
package jolokhd.fractalclock.wallpaper

class Vector2f(var x: Float, var y: Float){
    operator fun plus(foo: Vector2f): Vector2f {
        return Vector2f(x + foo.x,y + foo.y)
    }
    operator fun times(foo: Vector2f): Vector2f {
        return Vector2f(x * foo.x,y * foo.y)
    }
    operator fun times(foo: Float): Vector2f {
        return Vector2f(x * foo,y * foo)
    }
}
