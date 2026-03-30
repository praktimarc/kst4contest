import socket
import threading
import time
import random
import traceback
from datetime import datetime, timedelta

# =====================================
# KST-Server-Simulator / DO5AMF
# Usage: change configuration below and
# run. Enter 127.0.0.1 : 23001 as a
# target in KST4Contest or another
# KST chat client.
# =====================================

# ==========================================
# KONFIGURATION
# ==========================================

PORT = 23001
HOST = '127.0.0.1'

MSG_TO_USER_INTERVAL = 300.0 
LOGIN_LOGOUT_INTERVAL = 60.0 
KEEP_ALIVE_INTERVAL = 10.0 
CLIENT_WARMUP_TIME = 5.0 

PROB_INACTIVE = 0.10
PROB_REACTIVE = 0.20

# QSY Wahrscheinlichkeit (Wie oft wechselt ein User seine Frequenz?)
# 0.05 = 5% Chance pro Nachricht, dass er die Frequenz ändert. Sonst bleibt er stabil.
PROB_QSY = 0.05 

BANDS_VHF = { "2m": (144.150, 144.400), "70cm": (432.100, 432.300) }
BANDS_UHF = { "23cm": (1296.100, 1296.300), "3cm": (10368.100, 10368.250) }

CHANNELS_SETUP = {
    "2": {
        "NAME": "144/432 MHz",
        "NUM_USERS": 777,
        "BANDS": BANDS_VHF,
        "RATES": {"PUBLIC": 0.5, "DIRECTED": 3.0},
        "PERMANENT": [
            {"call": "DK5EW", "name": "Erwin", "loc": "JN47NX"},
            {"call": "DL1TEST", "name": "TestOp", "loc": "JO50XX"}
        ]
    },
    "3": {
        "NAME": "Microwave",
        "NUM_USERS": 333,
        "BANDS": BANDS_UHF,
        "RATES": {"PUBLIC": 0.2, "DIRECTED": 0.5},
        "PERMANENT": [
            {"call": "ON4KST", "name": "Alain", "loc": "JO20HI"},
            {"call": "G4CBW", "name": "MwTest", "loc": "IO83AA"} 
        ]
    }
}

COUNTRY_MAPPING = {
    "DL": ["JO", "JN"], "DA": ["JO", "JN"], "DF": ["JO", "JN"], "DJ": ["JO", "JN"], "DK": ["JO", "JN"], "DO": ["JO", "JN"],
    "F": ["JN", "IN", "JO"], "G": ["IO", "JO"], "M": ["IO", "JO"], "2E": ["IO", "JO"],
    "PA": ["JO"], "ON": ["JO"], "OZ": ["JO"], "SM": ["JO", "JP"], "LA": ["JO", "JP"],
    "OH": ["KP"], "SP": ["JO", "KO"], "OK": ["JO", "JN"], "OM": ["JN", "KN"],
    "HA": ["JN", "KN"], "S5": ["JN"], "9A": ["JN"], "HB9": ["JN"], "OE": ["JN"],
    "I": ["JN", "JM"], "IK": ["JN", "JM"], "IU": ["JN", "JM"], "EA": ["IN", "IM"],
    "CT": ["IM"], "EI": ["IO"], "GM": ["IO"], "GW": ["IO"], "YO": ["KN"],
    "YU": ["KN"], "LZ": ["KN"], "SV": ["KM", "KN"], "UR": ["KO", "KN"],
    "LY": ["KO"], "YL": ["KO"], "ES": ["KO"]
}

NAMES = ["Hans", "Peter", "Jo", "Alain", "Mike", "Sven", "Ole", "Jean", "Bob", "Tom", "Giovanni", "Mario", "Frank", "Steve", "Dave"]

MSG_TEMPLATES_WITH_FREQ = [
    "QSY {freq}", "PSE QSY {freq}", "Calling CQ on {freq}", "I am QRV on {freq}",
    "Listening on {freq}", "Can you try {freq}?", "Signals strong on {freq}",
    "Scattering on {freq}", "Please go to {freq}", "Running test on {freq}",
    "Any takers for {freq}?", "Back to {freq}", "QRG {freq}?", "Aircraft scatter {freq}"
]

MSG_TEMPLATES_TEXT_ONLY = [
    "TNX for QSO", "73 all", "Anyone for sked?", "Good conditions", 
    "Nothing heard", "Rain scatter?", "Waiting for moonrise", "CQ Contest", 
    "QRZ?", "My locator is {loc}", "Band is open"
]

REPLY_TEMPLATES = [
    "Hello {user}, 599 here", "Rgr {user}, tnx for report", "Yes {user}, QSY?", 
    "Sorry {user}, no copy", "Pse wait 5 min {user}", "Ok {user}, 73",
    "Locator is {loc}", "Go to {freq} please", "Rgr {user}, gl"
]

# ==========================================
# CLIENT WRAPPER
# ==========================================

class ConnectedClient:
    def __init__(self, sock, addr):
        self.sock = sock
        self.addr = addr
        self.call = f"GUEST_{random.randint(1000,9999)}"
        self.channels = {"2"} 
        self.login_time = time.time()
        self.lock = threading.Lock() 
        
    def send_safe(self, data_str):
        if not data_str: return True
        with self.lock:
            try:
                self.sock.sendall(data_str.encode('latin-1', errors='replace'))
                return True
            except:
                return False

    def close(self):
        try: self.sock.close()
        except: pass

# ==========================================
# LOGIK KLASSEN
# ==========================================

class MessageFactory:
    @staticmethod
    def get_stable_frequency(user, band_name, min_f, max_f):
        """Liefert eine stabile Frequenz für diesen User auf diesem Band"""
        # Wenn noch keine Frequenz da ist ODER Zufall zuschlägt (QSY)
        if band_name not in user['freqs'] or random.random() < PROB_QSY:
            freq_val = round(random.uniform(min_f, max_f), 3)
            user['freqs'][band_name] = f"{freq_val:.3f}"
            
        return user['freqs'][band_name]

    @staticmethod
    def get_chat_message(bands_config, user):
        try:
            # Entscheidung: Text mit Frequenz oder ohne?
            if random.random() < 0.7:
                # Wähle zufälliges Band aus den verfügbaren
                band_name = random.choice(list(bands_config.keys()))
                min_f, max_f = bands_config[band_name]
                
                # Hole STABILE Frequenz für diesen User
                freq_str = MessageFactory.get_stable_frequency(user, band_name, min_f, max_f)
                
                return random.choice(MSG_TEMPLATES_WITH_FREQ).format(freq=freq_str)
            else:
                return random.choice(MSG_TEMPLATES_TEXT_ONLY).format(loc=user['loc'])
        except: return "TNX 73"

    @staticmethod
    def get_reply_msg(bands, target_call, my_loc):
        try:
            tmpl = random.choice(REPLY_TEMPLATES)
            freq_str = "QSY?"
            # Bei Replies simulieren wir oft nur "QSY?" ohne konkrete Frequenz,
            # oder nutzen eine zufällige, da der Kontext fehlt.
            if "{freq}" in tmpl and bands:
                band_name = random.choice(list(bands.keys()))
                min_f, max_f = bands[band_name]
                freq_str = f"{round(random.uniform(min_f, max_f), 3):.3f}"
            return tmpl.format(user=target_call, loc=my_loc, freq=freq_str)
        except: return "TNX 73"

class UserFactory:
    registry = {} 

    @classmethod
    def get_or_create_user(cls, channel_id, current_channel_users):
        # 1. Reuse existing
        candidates = [u for call, u in cls.registry.items() if call not in current_channel_users]
        if candidates and random.random() < 0.5:
            return random.choice(candidates)
            
        # 2. Create new
        return cls._create_new_unique_user(channel_id, current_channel_users)

    @classmethod
    def _create_new_unique_user(cls, channel_id, current_channel_users):
        while True:
            prefix = random.choice(list(COUNTRY_MAPPING.keys()))
            num = random.randint(0, 9)
            suffix = "".join(random.choices("ABCDEFGHIJKLMNOPQRSTUVWXYZ", k=random.randint(1,3)))
            call = f"{prefix}{num}{suffix}"
            
            if call in current_channel_users: continue
            if call in cls.registry: return cls.registry[call]
                
            valid_grids = COUNTRY_MAPPING[prefix] 
            grid_prefix = random.choice(valid_grids)
            sq_num = f"{random.randint(0,99):02d}"
            sub = "".join(random.choices("ABCDEFGHIJKLMNOPQRSTUVWXYZ", k=2))
            loc = f"{grid_prefix}{sq_num}{sub}"
            
            name = random.choice(NAMES)
            rand = random.random()
            if rand < PROB_INACTIVE: role = "INACTIVE"
            elif rand < (PROB_INACTIVE + PROB_REACTIVE): role = "REACTIVE"
            else: role = "ACTIVE"
            
            # Neu V31: Frequenz-Gedächtnis
            user_data = {
                "call": call, 
                "name": name, 
                "loc": loc, 
                "role": role, 
                "freqs": {} # Speicher für { '2m': '144.300' }
            }
            
            cls.registry[call] = user_data
            return user_data

    @classmethod
    def register_permanent(cls, user_data):
        # Sicherstellen, dass auch Permanent User Freq-Memory haben
        if "freqs" not in user_data:
            user_data["freqs"] = {}
        cls.registry[user_data['call']] = user_data

# ==========================================
# CHANNEL INSTANCE
# ==========================================

class ChannelInstance:
    def __init__(self, cid, config, server):
        self.id = cid
        self.config = config
        self.server = server
        
        self.users_pool = []
        self.online_users = {} 
        self.history_chat = []
        
        self.last_pub = time.time()
        self.last_dir = time.time()
        self.last_me = time.time()
        self.last_login = time.time()
        
        self.rate_pub = 1.0 / config["RATES"]["PUBLIC"]
        self.rate_dir = 1.0 / config["RATES"]["DIRECTED"]
        
        self._init_data()
        
    def _init_data(self):
        print(f"[*] Init Channel {self.id} ({self.config['NAME']})...")
        
        for u in self.config["PERMANENT"]:
            u_full = u.copy()
            u_full["role"] = "ACTIVE"
            UserFactory.register_permanent(u_full)
            self.online_users[u['call']] = u_full

        for _ in range(self.config["NUM_USERS"]):
            new_u = UserFactory.get_or_create_user(self.id, self.online_users.keys())
            self.users_pool.append(new_u)
            
        fill = int(self.config["NUM_USERS"] * 0.9)
        for i in range(fill):
            u = self.users_pool[i]
            if u['call'] not in self.online_users:
                self.online_users[u['call']] = u
        
        print(f"[*] Channel {self.id} ready: {len(self.online_users)} Users.")
        self._prefill_history()
        
    def _prefill_history(self):
        actives = [u for u in self.online_users.values() if u['role'] == "ACTIVE"]
        if not actives: return
        start = datetime.now() - timedelta(minutes=15)
        for i in range(30):
            msg_time = start + timedelta(seconds=i*30)
            ts = str(int(msg_time.timestamp()))
            sender = random.choice(actives)
            if i % 2 == 0:
                text = MessageFactory.get_chat_message(self.config["BANDS"], sender)
                frame = f"CH|{self.id}|{ts}|{sender['call']}|{sender['name']}|0|{text}|0|\r\n"
            else:
                target = random.choice(list(self.online_users.values()))
                text = MessageFactory.get_reply_msg(self.config["BANDS"], target['call'], sender['loc'])
                frame = f"CH|{self.id}|{ts}|{sender['call']}|{sender['name']}|0|{text}|{target['call']}|\r\n"
            self.history_chat.append(frame)

    def tick(self, now):
        actives = [u for u in self.online_users.values() if u['role'] == "ACTIVE"]
        if not actives: return
        
        # PUBLIC
        if now - self.last_pub > self.rate_pub:
            self.last_pub = now
            u = random.choice(actives)
            # V31: Nutzt jetzt get_chat_message, das das Freq-Memory abfragt
            text = MessageFactory.get_chat_message(self.config["BANDS"], u)
            ts = str(int(now))
            frame = f"CH|{self.id}|{ts}|{u['call']}|{u['name']}|0|{text}|0|\r\n"
            self._add_hist(frame)
            self.server.broadcast_to_channel(self.id, frame)
            
        # DIRECTED
        if now - self.last_dir > self.rate_dir:
            self.last_dir = now
            if len(actives) > 5:
                u1 = random.choice(actives)
                u2 = random.choice(list(self.online_users.values()))
                if u1 != u2:
                    if random.random() < 0.5:
                        # Auch hier Frequenzstabilität beachten
                        text = MessageFactory.get_chat_message(self.config["BANDS"], u1)
                    else:
                        text = MessageFactory.get_reply_msg(self.config["BANDS"], u2['call'], u1['loc'])
                    ts = str(int(now))
                    frame = f"CH|{self.id}|{ts}|{u1['call']}|{u1['name']}|0|{text}|{u2['call']}|\r\n"
                    self.server.broadcast_to_channel(self.id, frame)
                    if u2['role'] != "INACTIVE":
                        threading.Thread(target=self._schedule_reply, args=(u2['call'], u1['call']), daemon=True).start()

        # MSG TO YOU
        if now - self.last_me > MSG_TO_USER_INTERVAL:
            self.last_me = now
            target_client = self.server.get_random_subscriber(self.id)
            if target_client and actives:
                if not target_client.call.startswith("GUEST"):
                    sender = random.choice(actives)
                    text = MessageFactory.get_chat_message(self.config["BANDS"], sender)
                    print(f"[SIM Ch{self.id}] MSG TO YOU ({target_client.call})")
                    self.process_msg(sender['call'], sender['name'], text, target_client.call)

        # LOGIN/LOGOUT
        if now - self.last_login > LOGIN_LOGOUT_INTERVAL:
            self.last_login = now
            if random.choice(['IN', 'OUT']) == 'OUT' and len(self.online_users) > 20:
                cands = [c for c in self.online_users if c not in [p['call'] for p in self.config["PERMANENT"]]]
                if cands:
                    l = random.choice(cands)
                    del self.online_users[l]
                    self.server.broadcast_to_channel(self.id, f"UR6|{self.id}|{l}|\r\n")
            else:
                candidates = [u for u in self.users_pool if u['call'] not in self.online_users]
                if candidates:
                    n = random.choice(candidates)
                    self.online_users[n['call']] = n
                    self.server.broadcast_to_channel(self.id, f"UA5|{self.id}|{n['call']}|{n['name']}|{n['loc']}|2|\r\n")

    def process_msg(self, sender, name, text, target):
        ts = str(int(time.time()))
        frame = f"CH|{self.id}|{ts}|{sender}|{name}|0|{text}|{target}|\r\n"
        if target == "0": self._add_hist(frame)
        self.server.broadcast_to_channel(self.id, frame)
        if target in self.online_users:
            threading.Thread(target=self._schedule_reply, args=(target, sender), daemon=True).start()
            
    def _schedule_reply(self, sim_sender, real_target):
        if sim_sender not in self.online_users: return
        u = self.online_users[sim_sender]
        if u['role'] == "INACTIVE": return
        
        time.sleep(random.uniform(2.0, 5.0))
        if sim_sender in self.online_users:
            text = MessageFactory.get_reply_msg(self.config["BANDS"], real_target, u['loc'])
            ts = str(int(time.time()))
            
            if self.server.is_real_user(real_target):
                print(f"[REPLY Ch{self.id}] {sim_sender} -> {real_target}")
            
            frame = f"CH|{self.id}|{ts}|{sim_sender}|{u['name']}|0|{text}|{real_target}|\r\n"
            self.server.broadcast_to_channel(self.id, frame)

    def _add_hist(self, frame):
        self.history_chat.append(frame)
        if len(self.history_chat) > 50: self.history_chat.pop(0)

    def get_full_init_blob(self):
        blob = ""
        for u in self.online_users.values():
            blob += f"UA0|{self.id}|{u['call']}|{u['name']}|{u['loc']}|0|\r\n"
        for h in self.history_chat: blob += h
        blob += f"UE|{self.id}|{len(self.online_users)}|\r\n"
        return blob.encode('latin-1', errors='replace')

# ==========================================
# SERVER
# ==========================================

class KSTServerV31:
    def __init__(self):
        self.lock = threading.Lock()
        self.running = True
        self.clients = {} 
        self.channels = {}
        
        for cid, cfg in CHANNELS_SETUP.items():
            self.channels[cid] = ChannelInstance(cid, cfg, self)
            
    def start(self):
        threading.Thread(target=self._sim_loop, daemon=True).start()
        
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            s.bind((HOST, PORT))
            s.listen(5)
            s.settimeout(1.0)
            print(f"[*] ON4KST V31 (Stable Frequencies) running on {HOST}:{PORT}")
            
            while self.running:
                try:
                    sock, addr = s.accept()
                    print(f"[*] CONNECT: {addr}")
                    threading.Thread(target=self._handle_client, args=(sock,), daemon=True).start()
                except socket.timeout: continue
                except OSError: break
        except KeyboardInterrupt:
            print("\n[!] Stop.")
        finally:
            self.running = False
            try: s.close()
            except: pass
            
    def _handle_client(self, sock):
        client_obj = ConnectedClient(sock, None)
        with self.lock:
            self.clients[sock] = client_obj
            
        buffer = ""
        try:
            while self.running:
                try: data = sock.recv(2048)
                except: break
                if not data: break
                
                buffer += data.decode('latin-1', errors='replace')
                while '\n' in buffer:
                    line, buffer = buffer.split('\n', 1)
                    line = line.strip()
                    if not line: continue
                    
                    parts = line.split('|')
                    cmd = parts[0]
                    
                    if cmd == 'LOGIN' or cmd == 'LOGINC':
                        if len(parts) > 1:
                            client_obj.call = parts[1].strip().upper()
                            print(f"[LOGIN] {client_obj.call} (Ch 2)")
                        
                        client_obj.send_safe(f"LOGSTAT|100|2|PySimV31|KEY|Conf|3|\r\n")
                        if cmd == 'LOGIN': 
                            self._send_channel_init(client_obj, "2")
                            
                    elif cmd == 'SDONE':
                        self._send_channel_init(client_obj, "2")
                        
                    elif cmd.startswith('ACHAT'):
                        if len(parts) >= 2:
                            new_chan = parts[1]
                            if new_chan in self.channels:
                                client_obj.channels.add(new_chan)
                                print(f"[ACHAT] {client_obj.call} -> Ch {new_chan}")
                                self._send_channel_init(client_obj, new_chan)
                                
                    elif cmd == 'MSG':
                        if len(parts) >= 4:
                            cid = parts[1]
                            target = parts[2]
                            text = parts[3]
                            if text.lower().startswith("/cq"):
                                spl = text.split(' ', 2)
                                if len(spl) >= 3:
                                    target = spl[1]; text = spl[2]
                            if cid in self.channels:
                                self.channels[cid].process_msg(client_obj.call, "Me", text, target)
                                
                    elif cmd == 'CK': pass
        except Exception as e:
            print(f"[!] Err: {e}")
        finally:
            with self.lock:
                if sock in self.clients: del self.clients[sock]
            client_obj.close()
            
    def _send_channel_init(self, client_obj, cid):
        if cid in self.channels:
            full_blob = self.channels[cid].get_full_init_blob()
            client_obj.send_safe(full_blob.decode('latin-1'))

    def broadcast_to_channel(self, cid, frame):
        now = time.time()
        with self.lock:
            targets = list(self.clients.values())
            
        for c in targets:
            if cid in c.channels:
                if now - c.login_time > CLIENT_WARMUP_TIME:
                    c.send_safe(frame)
                
    def get_random_subscriber(self, cid):
        with self.lock:
            subs = [c for c in self.clients.values() if cid in c.channels and not c.call.startswith("GUEST")]
        return random.choice(subs) if subs else None
        
    def is_real_user(self, call):
        with self.lock:
            for c in self.clients.values():
                if c.call.upper() == call.upper() and not c.call.startswith("GUEST"):
                    return True
        return False

    def _sim_loop(self):
        print("[*] Sim Loop running...")
        last_ka = time.time()
        while self.running:
            now = time.time()
            time.sleep(0.02)
            
            for c in self.channels.values():
                c.tick(now)
            
            if now - last_ka > KEEP_ALIVE_INTERVAL:
                last_ka = now
                self.broadcast_global("CK|\r\n") 
               
    def broadcast_global(self, frame):
        with self.lock:
            targets = list(self.clients.values())
        for c in targets:
            c.send_safe(frame)

if __name__ == '__main__':
    KSTServerV31().start()