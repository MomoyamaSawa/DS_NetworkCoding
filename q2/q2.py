import time
from urllib.parse import urlparse
import asyncio
import aiohttp
from bs4 import BeautifulSoup
import networkx as nx
import matplotlib.pyplot as plt

ROOT_URLS = [
    "https://www.tongji.edu.cn",
    "https://www.pku.edu.cn",
    "https://www.sina.com.cn",
    "https://www.mit.edu",
]
MAX_DEPTH = 4
SELECT_NUM = 6
MAX_ADDRESS_SIMILARITY = 2


async def fetch(session: aiohttp.ClientSession, url):
    async with session.get(url) as response:
        return await response.text()


def check_url(base, url):
    """
    检查该外部url域名不同于当前机构
    """
    base_domain = urlparse(base).netloc.split(".")
    url_domain = urlparse(url).netloc.split(".")
    return len(set(base_domain) & set(url_domain)) <= MAX_ADDRESS_SIMILARITY


async def main():
    G = nx.DiGraph()
    visited = set()
    queue = asyncio.Queue()
    for url in ROOT_URLS:
        # 这边的0是一个计数器，看看现在深度多少了
        queue.put_nowait((url, 0))
    async with aiohttp.ClientSession() as session:
        while not queue.empty():
            url, depth = await queue.get()
            if url in visited or depth > MAX_DEPTH:
                continue
            visited.add(url)
            try:
                html = await fetch(session, url)
                # 解析html
                soup = BeautifulSoup(html, "html.parser")
                # 从BeautifulSoup对象soup中提取出所有满足特定条件的链接。
                links = [
                    link.get("href")
                    for link in soup.find_all("a")
                    if link.get("href")
                    and link.get("href").startswith("http")
                    and check_url(url, link.get("href"))
                ]
                for link in links[:SELECT_NUM]:
                    G.add_edge(url, link)
                    queue.put_nowait((link, depth + 1))
            except Exception as e:
                print(f"An error occurred with URL {url}: {e}")
                continue
    draw_graph(G)


def draw_graph(G: nx.DiGraph):
    nx.draw(G, with_labels=True)
    plt.show()
    print(f"Number of nodes: {G.number_of_nodes()}")
    print(f"Number of edges: {G.number_of_edges()}")
    in_degrees = dict(G.in_degree())
    max_in_degree_node = max(in_degrees, key=in_degrees.get)
    print(
        f"Node with maximum in-degree: {max_in_degree_node} with in-degree {in_degrees[max_in_degree_node]}"
    )


start_time = time.time()
asyncio.run(main())
end_time = time.time()
print(f"Total time: {end_time - start_time} seconds")
