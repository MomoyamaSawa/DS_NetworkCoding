import time
from urllib.parse import urlparse
import asyncio
import async_timeout
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
MAX_DEPTH = 3
SELECT_NUM = 6
MAX_ADDRESS_SIMILARITY = 2
# 请求头
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
}
G = nx.DiGraph()
VISITED = set()
START = time.time()
END = time.time()


async def fetch(session: aiohttp.ClientSession, url, timeout=10):
    """
    爬虫
    """
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


async def start():
    """
    协程启动函数
    """
    global START
    print("Please select a root url to start:")
    for i, root in enumerate(ROOT_URLS):
        print(f"{i}. {root}")
    print(f"{len(ROOT_URLS)}. all thoes urls")

    while True:
        try:
            index = int(input("Enter the number of the url: "))
            if 0 <= index < len(ROOT_URLS):
                selected_url = ROOT_URLS[index]
                print(f"You selected: {selected_url}")
                break
            elif index == len(ROOT_URLS):
                selected_url = ROOT_URLS
                print(f"You selected: {selected_url}")
                break
            else:
                print("Invalid number, please try again.")
        except ValueError:
            print("Invalid input, please enter a number.")
    if isinstance(selected_url, list):
        for url in selected_url:
            asyncio.create_task(worker(url, 0))
    else:
        asyncio.create_task(worker(selected_url, 0))
    START = time.time()
    # 等待其他所有任务完成
    while True:
        if len(asyncio.all_tasks()) == 1:  # 只剩下当前任务
            break
        await asyncio.sleep(1)  # 等待一段时间后再检查


async def worker(url, depth):
    """
    递归工作函数
    """
    if url in VISITED or depth > MAX_DEPTH:
        print(f"[feature] {url} has been visited or out of max depth, skip")
        return
    VISITED.add(url)
    try:
        async with aiohttp.ClientSession() as session:
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
                asyncio.create_task(worker(link, depth + 1))
    except Exception as e:
        print(f"\033[95m[err]\033[0m With URL {url}: {e.__class__.__name__}, {e}")
        return


async def main():
    """
    非协程入口代码（弃用），我tm越看越傻逼，在协程里写队列进行循环这不是失去了协程的意义？我刚写这代码的时候脑子呢？？？应该递归添加task到协程循环里才对
    """
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
                    htmls = await asyncio.gather(fetch(session, url))
                    html = htmls[0]
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
    """
    画图
    """
    print(f"Number of nodes: {G.number_of_nodes()}")
    print(f"Number of edges: {G.number_of_edges()}")
    in_degrees = dict(G.in_degree())
    max_in_degree = max(in_degrees.values())
    max_in_degree_nodes = [
        node for node, in_degree in in_degrees.items() if in_degree == max_in_degree
    ]
    for node in max_in_degree_nodes:
        print(f"Node with maximum in-degree: {node} with in-degree {max_in_degree}")

    # 创建一个布局
    pos = nx.spring_layout(G)

    # 绘制节点
    nx.draw_networkx_nodes(G, pos, node_size=10)

    # 绘制边
    nx.draw_networkx_edges(G, pos, alpha=0.4)

    plt.show()


if __name__ == "__main__":
    print("\n")
    loop = asyncio.get_event_loop()
    loop.run_until_complete(start())
    END = time.time()
    print(
        "\n\n------------------------------------------------------------------------\n"
    )
    print(f"Total time: {END - START} seconds")
    draw_graph()
    print("\n")
