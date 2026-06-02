package com.ether4o4.morsvitaest.mcp

import androidx.compose.runtime.Immutable

@Immutable
data class PopularMcpServer(
    val name: String,
    val url: String,
    val description: String,
    val category: Category = Category.Other,
) {
    enum class Category(val label: String) {
        DevAndCode("Dev & code"),
        Productivity("Productivity"),
        SearchAndKnowledge("Search & knowledge"),
        DataAndMarkets("Data & markets"),
        WebAndContent("Web & content"),
        Other("Other"),
    }
}

val popularMcpServers = listOf(
    // Dev & code — biggest wedge for the "best mobile MCP client" pitch.
    PopularMcpServer(
        name = "HuggingFace",
        url = "https://hf.co/mcp",
        description = "Search models, datasets, papers, and spaces on Hugging Face from chat",
        category = PopularMcpServer.Category.DevAndCode,
    ),
    PopularMcpServer(
        name = "DeepWiki",
        url = "https://mcp.deepwiki.com/mcp",
        description = "AI-powered docs for any GitHub repo",
        category = PopularMcpServer.Category.DevAndCode,
    ),
    PopularMcpServer(
        name = "Context7",
        url = "https://mcp.context7.com/mcp",
        description = "Up-to-date library and framework docs",
        category = PopularMcpServer.Category.DevAndCode,
    ),
    PopularMcpServer(
        name = "Sequential Thinking",
        url = "https://remote.mcpservers.org/sequentialthinking/mcp",
        description = "Structured step-by-step problem-solving",
        category = PopularMcpServer.Category.DevAndCode,
    ),

    // Productivity / automation
    PopularMcpServer(
        name = "Zapier",
        url = "https://mcp.zapier.com/sse",
        description = "Connect to 6000+ apps (Slack, Notion, Linear, Gmail, …) via Zapier",
        category = PopularMcpServer.Category.Productivity,
    ),

    // Search & knowledge
    PopularMcpServer(
        name = "Jina AI",
        url = "https://mcp.jina.ai/v1",
        description = "Convert URLs to markdown, web search, image search",
        category = PopularMcpServer.Category.SearchAndKnowledge,
    ),
    PopularMcpServer(
        name = "Fetch",
        url = "https://remote.mcpservers.org/fetch/mcp",
        description = "Fetch web content and convert HTML to markdown",
        category = PopularMcpServer.Category.WebAndContent,
    ),

    // Data & markets
    PopularMcpServer(
        name = "CoinGecko",
        url = "https://mcp.api.coingecko.com/mcp",
        description = "Real-time crypto prices and market data",
        category = PopularMcpServer.Category.DataAndMarkets,
    ),
    PopularMcpServer(
        name = "Manifold Markets",
        url = "https://api.manifold.markets/v0/mcp",
        description = "Prediction market data and odds",
        category = PopularMcpServer.Category.DataAndMarkets,
    ),
    PopularMcpServer(
        name = "Open-Meteo Weather",
        url = "https://mcp.open-mcp.org/api/server/open-weather@latest/mcp",
        description = "Global weather forecasts and air quality",
        category = PopularMcpServer.Category.DataAndMarkets,
    ),

    // Other / utility
    PopularMcpServer(
        name = "Find-A-Domain",
        url = "https://api.findadomain.dev/mcp",
        description = "Domain availability across 1,444+ TLDs",
        category = PopularMcpServer.Category.Other,
    ),
    PopularMcpServer(
        name = "SubwayInfo NYC",
        url = "https://subwayinfo.nyc/mcp",
        description = "Real-time NYC transit info",
        category = PopularMcpServer.Category.Other,
    ),
)

/** External link to the canonical community-curated list of MCP servers. */
const val AWESOME_MCP_SERVERS_URL = "https://github.com/punkpeye/awesome-mcp-servers"
