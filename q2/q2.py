import time
from urllib.parse import urlparse
import asyncio
import async_timeout
import aiohttp
from bs4 import BeautifulSoup
import networkx as nx
import matplotlib.pyplot as plt

# ROOT_URLS = [
#     "https://www.tongji.edu.cn",
#     "https://www.pku.edu.cn",
#     "https://www.sina.com.cn",
#     "https://www.mit.edu",
# ]
ROOT_URLS = [
    "https://www.tongji.edu.cn",
]
MAX_DEPTH = 3
SELECT_NUM = 6
MAX_ADDRESS_SIMILARITY = 2
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
}
G = nx.DiGraph()


async def fetch(session: aiohttp.ClientSession, url, timeout=10):
    # 这边设置了10s的超时时间
    async with async_timeout.timeout(timeout):
        async with session.get(url, headers=HEADERS) as response:
            return await response.text()


def check_url(base, url):
    """
    检查该外部url域名不同于当前机构
    """
    base_domain = urlparse(base).netloc.split(".")
    url_domain = urlparse(url).netloc.split(".")
    test = len(set(base_domain) & set(url_domain)) <= MAX_ADDRESS_SIMILARITY
    if not test:
        print(f"[feature] {base} and {url} are similar")
        return False
    else:
        return True


def check_download_url(url):
    """
    检查URL是否指向下载文件
    """
    video_extensions = [".mp4", ".mp3", ".png", ".jpg", "apk"]
    url_path = urlparse(url).path
    test = any(url_path.endswith(ext) for ext in video_extensions)
    if test:
        print(f"\033[93m[Warning]\033[0m {url} is a download url, skip")
        return False
    else:
        return True


def check_ban_url(url):
    """
    检查URL是否指向了被墙的网站
    """
    ban_list = ["twitter", "youtube", "facebook"]  # 被墙网站列表
    for ban_word in ban_list:
        if ban_word in url:
            print(f"\033[93m[Warning]\033[0m {url} is a ban url in China, skip")
            return False
    return True


async def main():
    visited = set()
    queue = asyncio.Queue()
    for url in ROOT_URLS:
        # 这边的0是一个计数器，看看现在深度多少了
        queue.put_nowait((url, 0))
    start_time = time.time()
    async with aiohttp.ClientSession() as session:
        while True:
            # 获取当前事件循环中的所有任务
            tasks = asyncio.all_tasks()
            # 计算正在执行的任务数量
            running_tasks_count = sum(1 for task in tasks if not task.done())

            if running_tasks_count == 1 and queue.empty():
                break

            if not queue.empty():
                url, depth = await queue.get()

                if url in visited or depth > MAX_DEPTH:
                    print(f"[feature] {url} has been visited or out of max depth, skip")
                    continue
                visited.add(url)
                try:
                    html = await fetch(session, url)
                    # 解析html
                    soup = BeautifulSoup(html, "html.parser")
                    # 从BeautifulSoup对象soup中提取出所有满足特定条件的链接。
                    link_findings = soup.find_all("a")
                    if len(link_findings) == 0:
                        print(
                            f"\033[93m[Warning]\033[0m With URL {url}: no links found, Maybe the site has special treatment"
                        )
                    links = [
                        link.get("href")
                        for link in link_findings
                        if link.get("href")
                        and link.get("href").startswith("http")
                        and check_url(url, link.get("href"))
                        and check_download_url(link.get("href"))
                        and check_ban_url(link.get("href"))
                    ]
                    for link in links[:SELECT_NUM]:
                        link = link.replace("\r", "")  # 删除字符'\r'（ASCII 13）
                        G.add_edge(url, link)
                        print(f"\033[96m[info]\033[0m Add edge {url} -> {link}")
                        queue.put_nowait((link, depth + 1))
                except Exception as e:
                    print(
                        f"\033[95m[err]\033[0m With URL {url}: {e.__class__.__name__}, {e}"
                    )
                    continue

            else:
                # 如果队列为空，那么就等待一秒钟
                await asyncio.sleep(1)

    end_time = time.time()
    print(
        "\n\n------------------------------------------------------------------------\n"
    )
    print(f"Total time: {end_time - start_time} seconds")


def draw_graph():
    print(f"Number of nodes: {G.number_of_nodes()}")
    print(f"Number of edges: {G.number_of_edges()}")
    in_degrees = dict(G.in_degree())
    max_in_degree = max(in_degrees.values())
    max_in_degree_nodes = [
        node for node, in_degree in in_degrees.items() if in_degree == max_in_degree
    ]
    print("\n")
    for node in max_in_degree_nodes:
        print(f"Node with maximum in-degree: {node} with in-degree {max_in_degree}")
    nx.draw(G, with_labels=True)
    plt.show()


print("\n")
loop = asyncio.get_event_loop()
loop.run_until_complete(main())
draw_graph()
