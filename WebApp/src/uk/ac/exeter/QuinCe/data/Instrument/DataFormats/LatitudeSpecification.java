package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

/**
 * Specifies the latitude format for a data file
 * @author Steve Jones
 *
 */
public class LatitudeSpecification extends PositionSpecification {

	/**
	 * Indicates that latitudes are between -90 and 90
	 */
	public static final int FORMAT_MINUS90_90 = 0;
	
	/**
	 * Indicates that longitudes are between 0 and 90,
	 * with a separate column specifying the hemisphere
	 */
	public static final int FORMAT_0_90 = 1;
	
	/**
	 * Basic constructor
	 */
	public LatitudeSpecification() {
		super();
	}

	/**
	 * Constructor for a complete specification
	 * @param format The format
	 * @param valueColumn The value column
	 * @param hemisphereColumn The hemisphere column
	 * @throws PositionException If the specification is incomplete or invalid
	 */
	public LatitudeSpecification(int format, int valueColumn, int hemisphereColumn) throws PositionException {
		super(format, valueColumn, hemisphereColumn);
	}

	@Override
	public boolean formatValid(int format) {
		return (format >= 0 && format <= 1);
	}

	@Override
	public boolean hemisphereRequired() {
		return (getFormat() == FORMAT_0_90);
	}
	
}