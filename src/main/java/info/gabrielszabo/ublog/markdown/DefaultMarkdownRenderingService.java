package info.gabrielszabo.ublog.markdown;

public class DefaultMarkdownRenderingService implements MarkdownRenderingService {

    @Override
    public String renderToHtml(String markdownContent) {
        // Simple Markdown to HTML conversion logic (for demonstration purposes)
        String htmlContent = markdownContent.replaceAll("(?m)^### (.+)$", "<h3>$1</h3>")
                                            .replaceAll("(?m)^## (.+)$", "<h2>$1</h2>")
                                            .replaceAll("(?m)^# (.+)$", "<h1>$1</h1>")
                                            .replaceAll("(?m)\\*(.+?)\\*", "<em>$1</em>")
                                            .replaceAll("(?m)\\*(.+?)\\*", "<strong>$1</strong>");
        return htmlContent;
    }

}
