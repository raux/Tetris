package tetrads;


public class Straight extends Tetrad {

	public Straight() {
		super(3, 0, createOrientation("STRAIGHT"));
	}
	
	@Override
	public Tetrads getType() {
		return Tetrads.STRAIGHT;
	}

}
