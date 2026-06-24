import os
import sys
import subprocess
import threading
import time
import webbrowser
import socket
import shutil

BASE = os.path.dirname(os.path.abspath(__file__))

def get_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except:
        return "localhost"

def instalar_dependencias():
    print("[1/3] Instalando dependencias...")
    subprocess.check_call([
        sys.executable, "-m", "pip", "install",
        "pystray", "pillow", "--quiet", "--disable-pip-version-check"
    ])
    print("OK - pystray e pillow instalados!")

def criar_pastas():
    print("[2/3] Criando pastas...")
    for p in ["lanchonete", "lanchonete/data", "lanchonete/imagens", "interface"]:
        os.makedirs(os.path.join(BASE, p), exist_ok=True)
    src = os.path.join(BASE, "index.html")
    dst = os.path.join(BASE, "interface", "index.html")
    if os.path.exists(src) and not os.path.exists(dst):
        shutil.copy(src, dst)
    print("OK - Pastas criadas!")

def criar_atalho():
    print("[3/3] Criando atalho na area de trabalho...")
    try:
        desktop = os.path.join(os.path.expanduser("~"), "Desktop")
        tray    = os.path.join(BASE, "mana.py")
        pythonw = os.path.join(os.path.dirname(sys.executable), "pythonw.exe")
        if not os.path.exists(pythonw):
            pythonw = sys.executable
        lnk = os.path.join(desktop, "Mana Hamburgueria.lnk")
        ps_cmd = (
            "$ws = New-Object -ComObject WScript.Shell; "
            "$s = $ws.CreateShortcut('" + lnk + "'); "
            "$s.TargetPath = '" + pythonw + "'; "
            "$s.Arguments = '\"" + tray + "\"'; "
            "$s.WorkingDirectory = '" + BASE + "'; "
            "$s.IconLocation = 'shell32.dll,137'; "
            "$s.Save()"
        )
        subprocess.run(["powershell", "-NoProfile", "-Command", ps_cmd], check=True)
        if os.path.exists(lnk):
            print("OK - Atalho criado na area de trabalho!")
        else:
            print("AVISO: Atalho nao criado. Use INICIAR.bat para abrir.")
    except Exception as e:
        print("AVISO: Nao foi possivel criar atalho: " + str(e))

def iniciar_sistema():
    sys.path.insert(0, BASE)
    import server as srv
    PORT = srv.PORT
    ip   = get_ip()
    url  = "http://localhost:" + str(PORT)
    urlr = "http://" + ip + ":" + str(PORT)

    srv.init_database()
    httpd = srv.HTTPServer(("0.0.0.0", PORT), srv.ManaFoodHandler)
    threading.Thread(target=httpd.serve_forever, daemon=True).start()

    # Inicia Cloudflare Tunnel automaticamente (se cloudflared existir)
    tunnel_exe = os.path.join(BASE, "cloudflared-windows-amd64.exe")
    if os.path.exists(tunnel_exe):
        def _run_tunnel():
            try:
                proc = subprocess.Popen(
                    [tunnel_exe, "tunnel", "--url", "http://localhost:" + str(PORT)],
                    stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                    cwd=BASE, creationflags=0x08000000
                )
                for line in iter(proc.stdout.readline, b''):
                    txt = line.decode('utf-8', errors='ignore').strip()
                    if 'trycloudflare.com' in txt and 'https://' in txt:
                        # Extrai o link do tunnel
                        import re
                        m = re.search(r'(https://[a-z0-9-]+\.trycloudflare\.com)', txt)
                        if m:
                            global tunnel_url
                            tunnel_url = m.group(1)
                            print("Tunnel  : " + tunnel_url)
                            print("Cardapio: " + tunnel_url + "/cardapio")
            except: pass
        threading.Thread(target=_run_tunnel, daemon=True).start()

    time.sleep(1.5)
    webbrowser.open(url)

    print("Sistema rodando!")
    print("Este PC  : " + url)
    print("Celular  : " + urlr)

    try:
        from PIL import Image, ImageDraw
        import pystray

        def make_icon():
            img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
            d = ImageDraw.Draw(img)
            d.ellipse([4,  4, 60, 26], fill="#e67e22")
            d.rectangle([4, 25, 60, 31], fill="#27ae60")
            d.rectangle([4, 30, 60, 40], fill="#922b21")
            d.rectangle([4, 39, 60, 44], fill="#f1c40f")
            d.rectangle([4, 43, 60, 58], fill="#e67e22")
            return img

        def abrir(icon, item):
            webbrowser.open(url)

        def ver_info(icon, item):
            def _show():
                import tkinter as tk
                from tkinter import messagebox
                r = tk.Tk()
                r.withdraw()
                r.attributes('-topmost', True)
                turl = globals().get('tunnel_url','(iniciando...)')
                messagebox.showinfo(
                    "Mana Food — Endereços",
                    "✅ Sistema rodando!\n\n"
                    "💻 Este PC:\n" + url + "\n\n"
                    "📱 Celular (Wi-Fi):\n" + urlr + "\n\n"
                    "🌐 Link externo:\n" + turl + "\n\n"
                    "🍔 Cardápio:\n" + turl + "/cardapio"
                )
                r.destroy()
            threading.Thread(target=_show, daemon=True).start()

        def copiar_link(icon, item):
            def _copy():
                import tkinter as tk
                r = tk.Tk()
                r.withdraw()
                r.attributes('-topmost', True)
                r.clipboard_clear()
                r.clipboard_append(urlr)
                r.update()
                time.sleep(0.5)
                r.destroy()
            threading.Thread(target=_copy, daemon=True).start()

        def encerrar(icon, item):
            icon.stop()
            os._exit(0)

        menu = pystray.Menu(
            pystray.MenuItem("Abrir no navegador", abrir, default=True),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem("Ver enderecos",      ver_info),
            pystray.MenuItem("Copiar link Wi-Fi",  copiar_link),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem("Encerrar",           encerrar),
        )

        icon = pystray.Icon(
            "mana",
            make_icon(),
            "Mana Hamburgueria | " + urlr,
            menu
        )
        icon.run()

    except Exception as e:
        print("Bandeja indisponivel, abrindo janela simples...")
        import tkinter as tk
        from tkinter import messagebox
        r = tk.Tk()
        r.withdraw()
        messagebox.showinfo(
            "Mana Hamburgueria - Rodando",
            "Sistema rodando!\n\nEste PC:\n" + url +
            "\n\nCelular/Tablet (Wi-Fi):\n" + urlr +
            "\n\nClique OK para ENCERRAR o sistema."
        )
        httpd.shutdown()
        os._exit(0)

if __name__ == "__main__":
    if "--instalar" in sys.argv:
        print("=== INSTALANDO MANA HAMBURGUERIA ===")
        instalar_dependencias()
        criar_pastas()
        criar_atalho()
        print("")
        print("=== INSTALACAO CONCLUIDA! ===")
        print("Iniciando o sistema...")
        print("")
    iniciar_sistema()
