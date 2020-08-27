package dk.aau.cs.shacl.utils;

import java.util.Objects;

public class Tuple4<X, Y, Z, K> {
    public final X _1;
    public final Y _2;
    public final Z _3;
    public final K _4;
    public Tuple4(X _1, Y _2, Z _3, K _4) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
        this._4 = _4;
    }
    public X _1() {
        return _1;
    }
    public Y _2() {
        return _2;
    }
    public Z _3() { return _3; }
    public K _4() { return _4; }
    @Override
    public String toString() {
        return "dk.aau.cs.shacl.utils.Tuple3{" +
                "_1=" + _1 +
                ", _2=" + _2 +
                ", _3=" + _3 +
                ", _4=" + _4 +
                '}';
    }
    public int hashCode() {
        return Objects.hash(_1,_2,_3, _4);
    }
    @Override
    public boolean equals(Object o) {
        if (o instanceof Tuple4) {
            final Tuple4 other = (Tuple4)o;
            return Objects.equals(_1,other._1) && Objects.equals(_2,other._2) && Objects.equals(_3,other._3)  && Objects.equals(_4,other._4);
        }
        else {
            return false;
        }
    }
}