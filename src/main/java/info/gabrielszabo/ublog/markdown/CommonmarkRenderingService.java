package info.gabrielszabo.ublog.markdown;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class CommonmarkRenderingService implements MarkdownRenderingService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    
    public CommonmarkRenderingService() {
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder().build();
    }

    public String renderToHtml(String markdown) {
        Node document = parser.parse(markdown);
        String html = renderer.render(document);
        return html;
    }

}
