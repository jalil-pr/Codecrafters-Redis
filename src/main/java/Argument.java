import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Argument {
     private final String regexLength = "\\$\\d+";
  private final String regexLengthGroup = "\\$(\\d+)";
  private String lenghtString;
  private int length;
  private String argument;
  public Argument(String lenght, String argument) {
    this.argument = argument;
    this.lenghtString = lenght;
    if (this.lenghtString.matches(regexLength)) {
      Pattern pattern = Pattern.compile(regexLengthGroup);
      Matcher matcher = pattern.matcher(this.lenghtString);
      matcher.find();
      this.length = Integer.parseInt(matcher.group(1));
    } else {
      new Exception("Length text does not match '$number' format");
    }
  }
  public int getLength() { return length; }
  public String getArgument() { return argument; }

  public String getLenghtString() { return lenghtString; }
}
