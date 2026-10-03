import lombok.Getter;
import lombok.Setter;

@Getter
public class TextCorrection {
    private final String originalText;

    @Setter
    private String correctedText;

    @Setter
    private String explanation;

    public TextCorrection(String originalText) {
        this.originalText = originalText;
    }
}
