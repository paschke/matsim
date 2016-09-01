package playground.paschke.utils;

public class ConvertKml {
	public static void main(String[] args) {
		KmlReader KmlReader = new KmlReader();
		XmlWriter XmlWriter = new XmlWriter();

		KmlReader.readFile(args[0]);
		XmlWriter.write(args[1], KmlReader.getRelocationZones());
	}
}
