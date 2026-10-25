package info.gabrielszabo.ublog.markdown;

import java.util.List;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class CommonmarkRenderingService implements MarkdownRenderingService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    
   public CommonmarkRenderingService() {
        List<Extension> extensions = List.of(TablesExtension.create());

        this.parser = Parser.builder()
                .extensions(extensions)
                .build();

        this.renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build();
    }

    @Override
    public String renderToHtml(String markdown) {
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

}
