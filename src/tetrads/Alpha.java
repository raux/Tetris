package tetrads;


public class Alpha extends Tetrad {
	
	public Alpha() {
		super(3, 0, createOrientation("ALPHA"));
	}

	@Override
	public Tetrads getType() {
		return Tetrads.ALPHA;
	}

}
