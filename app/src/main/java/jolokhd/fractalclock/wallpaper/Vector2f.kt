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