package info.gabrielszabo.ublog.markdown;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import info.gabrielszabo.ublog.log.LogService;

public class CommonmarkRenderingService implements MarkdownRenderingService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final LogService logService;

    public CommonmarkRenderingService(LogService logService) {
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder().build();
        this.logService = logService;
    }

    public String renderToHtml(String markdown) {
        logService.logTrace("Rendering markdown to HTML");
        Node document = parser.parse(markdown);
        String html = renderer.render(document);
        logService.logTrace("Rendered markdown to HTML");
        return html;
    }

}
