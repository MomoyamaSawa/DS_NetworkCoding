from enum import Enum
import time
from urllib.parse import urlparse
import asyncio
import async_timeout
import aiohttp
from bs4 import BeautifulSoup
import networkx as nx
import matplotlib.pyplot as plt
from colorama import init, Fore

init(autoreset=True)


ROOT_URLS = [
    "https://www.tongji.edu.cn",
    "https://www.pku.edu.cn",
    "https://www.sina.com.cn",
    "https://www.mit.edu",
]
BAN_LIST = ["twitter", "youtube", "facebook"]
DOWNLOAD_LIST = [".mp4", ".mp3", ".png", ".jpg", "apk"]
MAX_DEPTH = 3
SELECT_NUM = 6
MAX_ADDRESS_SIMILARITY = 2
# 浏览器请求头
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
}
G = nx.DiGraph()
VISITED = set()


class MsgColorEnum(Enum):
    """
    日志信息
    """

    feature = f"{Fore.RESET}[feature]"
    info = f"{Fore.LIGHTCYAN_EX}[info]"
    err = f"{Fore.LIGHTMAGENTA_EX}[err]"
    warning = f"{Fore.LIGHTYELLOW_EX}[Warning]"


def print_msg(msg_type, msg):
    print(f"{msg_type.value} {msg}")


async def fetch(session: aiohttp.ClientSession, url, timeout=7):
    """
    爬虫
    """
    # 这边设置了7s的超时时间
    async with async_timeout.timeout(timeout):
        async with session.get(url, headers=HEADERS) as response:
            return await response.text()


def check_url(base, url):
    """
    检查url合规性
    """
    # 检查url域名与当前域名的相似度
    base_domain = urlparse(base).netloc.split(".")
    url_domain = urlparse(url).netloc.split(".")
    test = len(set(base_domain) & set(url_domain)) <= MAX_ADDRESS_SIMILARITY
    if not test:
        print_msg(MsgColorEnum.feature, f"{base} and {url} are similar")
        return False
    # 检查url是否指向下载文件
    url_path = urlparse(url).path
    test = any(url_path.endswith(ext) for ext in DOWNLOAD_LIST)
    if test:
        print_msg(MsgColorEnum.warning, f"{url} is a download url, skip")
        return False
    # 检查url是否指向了被墙的网站
    for ban_word in BAN_LIST:
        if ban_word in url:
            print_msg(MsgColorEnum.warning, f"{url} is a ban url in China, skip")
            return False
    return True


async def start_async(urls):
    """
    协程启动函数
    """
    for url in urls:
        asyncio.create_task(worker(url, 0))
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
        print_msg(
            MsgColorEnum.feature, f"{url} has been visited or out of max depth, skip"
        )
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
                print_msg(MsgColorEnum.warning, f"With URL {url}: no links found")
            links = [
                link.get("href")
                for link in link_findings
                if link.get("href")
                and link.get("href").startswith("http")
                and check_url(url, link.get("href"))
            ]
            for link in links[:SELECT_NUM]:
                link = link.replace(
                    "\r", ""
                )  # 删除字符'\r'（ASCII 13）测试的时候读到的网址莫名其妙有这个字符不知道为啥
                G.add_edge(url, link)
                print_msg(MsgColorEnum.info, f"Add edge {url} -> {link}")
                asyncio.create_task(worker(link, depth + 1))
    except Exception as e:
        print_msg(MsgColorEnum.err, f"With URL {url}: {e.__class__.__name__}, {e}")
        return


def has_href(tag):
    return tag.has_attr("href")


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


def select():
    print("\nPlease select the option to start:")
    print("0. all thoes urls below")
    selected_urls = []
    for i, root in enumerate(ROOT_URLS):
        print(f"{i+1}. {root}")
    while True:
        try:
            index = int(input("Enter the number of the url: "))
            if 0 < index <= len(ROOT_URLS):
                selected_urls.append(ROOT_URLS[index])
                print(f"You selected: {selected_urls}")
                break
            elif index == 0:
                selected_urls = ROOT_URLS.copy()
                print(f"You selected: {selected_urls}")
                break
            else:
                print("Invalid number, please try again.")
        except ValueError:
            print("Invalid input, please enter a number.")
    return selected_urls


if __name__ == "__main__":
    selects = select()
    loop = asyncio.get_event_loop()
    start = time.time()
    loop.run_until_complete(start_async(selects))
    end = time.time()
    print("\n\n---------------------------------------------------------------------\n")
    print(f"Total time: {end - start} seconds")
    draw_graph()
    print("\n")


# async def main():
#     """
#     非协程入口代码（弃用），我tm越看越傻逼，在协程里写队列进行循环这不是失去了协程的意义？我刚写这代码的时候脑子呢？？？应该递归添加task到协程循环里才对
#     """
#     visited = set()
#     queue = asyncio.Queue()
#     for url in ROOT_URLS:
#         # 这边的0是一个计数器，看看现在深度多少了
#         queue.put_nowait((url, 0))
#     start_time = time.time()
#     async with aiohttp.ClientSession() as session:
#         while True:
#             # 获取当前事件循环中的所有任务
#             tasks = asyncio.all_tasks()
#             # 计算正在执行的任务数量
#             running_tasks_count = sum(1 for task in tasks if not task.done())

#             if running_tasks_count == 1 and queue.empty():
#                 break

#             if not queue.empty():
#                 url, depth = await queue.get()

#                 if url in visited or depth > MAX_DEPTH:
#                     print(f"[feature] {url} has been visited or out of max depth, skip")
#                     continue
#                 visited.add(url)
#                 try:
#                     htmls = await asyncio.gather(fetch(session, url))
#                     html = htmls[0]
#                     # 解析html
#                     soup = BeautifulSoup(html, "html.parser")
#                     # 从BeautifulSoup对象soup中提取出所有满足特定条件的链接。
#                     link_findings = soup.find_all(has_href)
#                     if len(link_findings) == 0:
#                         print(
#                             f"\033[93m[Warning]\033[0m With URL {url}: no links found, Maybe the site has special treatment"
#                         )
#                     links = [
#                         link.get("href")
#                         for link in link_findings
#                         if link.get("href")
#                         and link.get("href").startswith("http")
#                         and check_url(url, link.get("href"))
#                         and check_download_url(link.get("href"))
#                         and check_ban_url(link.get("href"))
#                     ]
#                     for link in links[:SELECT_NUM]:
#                         link = link.replace("\r", "")  # 删除字符'\r'（ASCII 13）
#                         G.add_edge(url, link)
#                         print(f"\033[96m[info]\033[0m Add edge {url} -> {link}")
#                         queue.put_nowait((link, depth + 1))
#                 except Exception as e:
#                     print(
#                         f"\033[95m[err]\033[0m With URL {url}: {e.__class__.__name__}, {e}"
#                     )
#                     continue

#             else:
#                 # 如果队列为空，那么就等待一秒钟
#                 await asyncio.sleep(1)

#     end_time = time.time()
#     print(
#         "\n\n------------------------------------------------------------------------\n"
#     )
#     print(f"Total time: {end_time - start_time} seconds")
