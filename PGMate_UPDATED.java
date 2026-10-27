import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

// ============================================================
// PGMate - SUPER SIMPLE version (for students to explain)
// Run: javac PGMate.java && java PGMate
// Open: http://localhost:8080
//
// Room rule: Floor 1 -> 101,102,103... Floor 2 -> 201,202... Floor 3 -> 301...
// Click any room number to see full details.
// ============================================================
public class PGMate {

    // ---------- 1. ABSTRACTION (abstract class) ----------
    static abstract class Person {
        String id, name, phone;
        Person(String id, String name, String phone) { this.id = id; this.name = name; this.phone = phone; }
        abstract String info(); // child must write this
    }

    // ---------- 2. INTERFACE (small contract) ----------
    interface Rentable { void pay(double amt); }

    // ---------- 3. INHERITANCE (Tenant is a Person) ----------
    static class Tenant extends Person implements Rentable {
        String roomNo = ""; // "" = no room
        double totalPaid = 0;

        // easy constructor
        Tenant(String id, String name, String phone) { super(id, name, phone); }

        String info() { return name + " (" + phone + ") Room:" + (roomNo.equals("") ? "None" : roomNo); }
        public void pay(double amt) { totalPaid += amt; } // from interface
    }

    // ---------- 4. CLASS + OBJECT (Room holds tenants) ----------
    static class Room {
        String no;          // e.g. "101"
        String type;        // Single, Double, Triple, Four
        double rent;
        ArrayList<String> members = new ArrayList<>(); // tenant ids in this room

        Room(String no, String type, double rent) { this.no = no; this.type = type; this.rent = rent; }

        int floor() { return no.charAt(0) - '0'; } // "101" -> 1, "201" -> 2

        int capacity() { // beds from type
            if (type.equals("Single")) return 1;
            if (type.equals("Double")) return 2;
            if (type.equals("Triple")) return 3;
            return 4; // Four
        }
        int filled() { return members.size(); }
        int vacant() { return capacity() - members.size(); }
        String status() {
            if (members.size() == 0) return "Available";
            if (members.size() < capacity()) return "Partial";
            return "Full";
        }
    }

    static class Payment {
        String id, tenantId, month, date; double amount;
        Payment(String id, String t, double a, String m) { this.id = id; tenantId = t; amount = a; month = m; date = LocalDate.now().toString(); }
    }

    static class Complaint {
        String id, tenantId, text, status = "Pending", date = LocalDate.now().toString();
        Complaint(String id, String t, String x) { this.id = id; tenantId = t; text = x; }
    }

    // ---------- 5. EXCEPTIONS (our own errors) ----------
    static class RoomFull extends Exception { RoomFull(String m) { super(m); } }
    static class NotFound extends Exception { NotFound(String m) { super(m); } }

    // ---------- 6. DATA (COLLECTIONS = ArrayList) ----------
    static ArrayList<Room> rooms = new ArrayList<>();
    static ArrayList<Tenant> tenants = new ArrayList<>();
    static ArrayList<Payment> payments = new ArrayList<>();
    static ArrayList<Complaint> complaints = new ArrayList<>();
    static int tSeq = 1, pSeq = 1, cSeq = 1;

    // helper: find room by number (simple loop, easy to explain)
    static Room findRoom(String no) {
        for (Room r : rooms) if (r.no.equals(no)) return r;
        return null;
    }
    static Tenant findTenant(String id) {
        for (Tenant t : tenants) if (t.id.equals(id)) return t;
        return null;
    }
    static String curMonth() { return LocalDate.now().toString().substring(0, 7); } // "2026-10"

    // GENERICS example (one small generic method - easy): filter any list
    static <T> ArrayList<T> filter(ArrayList<T> list, java.util.function.Predicate<T> cond) {
        ArrayList<T> out = new ArrayList<>();
        for (T x : list) if (cond.test(x)) out.add(x);
        return out;
    }

    // ---------- ACTIONS ----------
    static Room addRoom(String no, String type, double rent) throws Exception {
        if (!no.matches("[1-9][0-9]{2}")) throw new Exception("Room must be 3 digits like 101 (floor 1), 201 (floor 2)");
        if (findRoom(no) != null) throw new Exception("Room " + no + " already exists");
        Room r = new Room(no, type, rent);
        rooms.add(r); save(); return r;
    }

    static Tenant addTenant(String name, String phone) throws Exception {
        if (name.trim().equals("")) throw new Exception("Name empty");
        Tenant t = new Tenant("T" + String.format("%03d", tSeq++), name.trim(), phone.trim());
        tenants.add(t); save(); return t;
    }

    static void allocate(String tid, String rno) throws Exception {
        Tenant t = findTenant(tid); if (t == null) throw new NotFound("Tenant not found");
        Room r = findRoom(rno); if (r == null) throw new NotFound("Room not found");
        if (r.filled() >= r.capacity()) throw new RoomFull("Room " + rno + " is Full (" + r.filled() + "/" + r.capacity() + ")");
        if (!t.roomNo.equals("")) throw new Exception(t.name + " already in Room " + t.roomNo + ". Vacate first.");
        r.members.add(tid); t.roomNo = rno; save();
    }

    static void vacate(String tid) throws Exception {
        Tenant t = findTenant(tid); if (t == null) throw new NotFound("Tenant not found");
        Room r = findRoom(t.roomNo);
        if (r != null) r.members.remove(tid);
        t.roomNo = ""; save();
    }

    // ---------- FILE HANDLING (save/load text files) ----------
    static void save() {
        try {
            Files.createDirectories(Paths.get("data"));
            BufferedWriter w = Files.newBufferedWriter(Paths.get("data/rooms.txt"));
            for (Room r : rooms) w.write(r.no + "|" + r.type + "|" + r.rent + "|" + String.join(",", r.members) + "\n");
            w.close();
            w = Files.newBufferedWriter(Paths.get("data/tenants.txt"));
            for (Tenant t : tenants) w.write(t.id + "|" + t.name + "|" + t.phone + "|" + t.roomNo + "|" + t.totalPaid + "\n");
            w.close();
            w = Files.newBufferedWriter(Paths.get("data/payments.txt"));
            for (Payment p : payments) w.write(p.id + "|" + p.tenantId + "|" + p.amount + "|" + p.month + "|" + p.date + "\n");
            w.close();
            w = Files.newBufferedWriter(Paths.get("data/complaints.txt"));
            for (Complaint c : complaints) w.write(c.id + "|" + c.tenantId + "|" + c.text.replace("|", " ") + "|" + c.status + "|" + c.date + "\n");
            w.close();
        } catch (Exception e) { System.out.println("Save error: " + e.getMessage()); }
    }

    static void load() {
        try {
            rooms.clear(); tenants.clear(); payments.clear(); complaints.clear();
            if (!Files.exists(Paths.get("data/rooms.txt"))) { seed(); return; }
            for (String line : Files.readAllLines(Paths.get("data/rooms.txt"))) {
                if (line.trim().equals("")) continue;
                String[] p = line.split("\\|", -1);
                // OLD file had 6 parts: no|type|rent|floor|true|false|tenantId -> convert
                if (p.length >= 6 && (p[4].equals("true") || p[4].equals("false"))) {
                    Room r = new Room(p[0], p[1], Double.parseDouble(p[2]));
                    if (p[4].equals("true") && !p[5].trim().equals("")) r.members.add(p[5].trim());
                    rooms.add(r);
                } else {
                    // NEW file: no|type|rent|id1,id2
                    Room r = new Room(p[0], p[1], Double.parseDouble(p[2]));
                    if (p.length > 3 && !p[3].trim().equals(""))
                        for (String id : p[3].split(",")) if (!id.trim().equals("")) r.members.add(id.trim());
                    rooms.add(r);
                }
            }
            int mx = 0;
            if (Files.exists(Paths.get("data/tenants.txt")))
            for (String line : Files.readAllLines(Paths.get("data/tenants.txt"))) {
                if (line.trim().equals("")) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 5) continue;
                Tenant t = new Tenant(p[0], p[1], p[2]); t.roomNo = p[3]; t.totalPaid = Double.parseDouble(p[4]);
                tenants.add(t);
                try { mx = Math.max(mx, Integer.parseInt(p[0].replaceAll("[^0-9]", ""))); } catch (Exception ignored) {}
            }
            tSeq = mx + 1;
            if (Files.exists(Paths.get("data/payments.txt")))
            for (String line : Files.readAllLines(Paths.get("data/payments.txt"))) {
                if (line.trim().equals("")) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 5) continue;
                Payment pay = new Payment(p[0], p[1], Double.parseDouble(p[2]), p[3]); pay.date = p[4];
                payments.add(pay);
                try { pSeq = Math.max(pSeq, Integer.parseInt(p[0].replaceAll("[^0-9]", "")) + 1); } catch (Exception ignored) {}
            }
            if (Files.exists(Paths.get("data/complaints.txt")))
            for (String line : Files.readAllLines(Paths.get("data/complaints.txt"))) {
                if (line.trim().equals("")) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 5) continue;
                Complaint c = new Complaint(p[0], p[1], p[2]); c.status = p[3]; c.date = p[4];
                complaints.add(c);
                try { cSeq = Math.max(cSeq, Integer.parseInt(p[0].replaceAll("[^0-9]", "")) + 1); } catch (Exception ignored) {}
            }
            repair(); // keep rooms <-> tenants always connected
        } catch (Exception e) { System.out.println("Load: " + e.getMessage()); seed(); }
    }

    // Keep Room.members and Tenant.roomNo always in sync (fixes vacant-but-no-tenant bug)
    static void repair() {
        // 1. remove fake ids from rooms (ids with no tenant)
        for (Room r : rooms) r.members.removeIf(id -> findTenant(id) == null);
        // 2. if tenant says Room 102 but room list misses him -> add him
        for (Tenant t : tenants) {
            if (!t.roomNo.equals("")) {
                Room r = findRoom(t.roomNo);
                if (r == null) t.roomNo = ""; // room deleted -> free tenant
                else if (!r.members.contains(t.id) && r.filled() < r.capacity()) r.members.add(t.id);
                else if (!r.members.contains(t.id)) t.roomNo = ""; // room full -> free tenant
            }
        }
        // 3. if data got fully empty by mistake but rooms show filled -> reseed once
        if (tenants.size() == 0 && payments.size() == 0) {
            boolean anyFilled = false;
            for (Room r : rooms) if (r.filled() > 0) anyFilled = true;
            if (anyFilled || rooms.size() == 0) seed();
            else save();
        } else save();
    }

    static void seed() {
        try {
            rooms.clear(); tenants.clear(); payments.clear(); complaints.clear();
            addRoom("101", "Single", 6000);
            addRoom("102", "Double", 4500);
            addRoom("103", "Double", 4500);
            addRoom("201", "Double", 4500);
            addRoom("202", "Four", 3000);
            Tenant a = addTenant("Aarav Patel", "9876543210");
            Tenant b = addTenant("Diya Shah", "9898989898");
            Tenant c = addTenant("Rohan Mehta", "9777777777");
            allocate(a.id, "101");
            allocate(b.id, "102");
            allocate(c.id, "102"); // double room now full
            payments.add(new Payment("P0001", a.id, 6000, curMonth())); a.pay(6000);
            complaints.add(new Complaint("C001", b.id, "WiFi slow on 1st floor"));
            save();
        } catch (Exception e) { System.out.println("Seed: " + e.getMessage()); }
    }

    // ---------- WEBSITE (just copy-paste starter, no need to explain deeply) ----------
    public static void main(String[] args) throws Exception {
        load();
        HttpServer s = null; int port = 8080;
        while (s == null && port < 8091) {
            try { s = HttpServer.create(new InetSocketAddress(port), 0); }
            catch (BindException e) { System.out.println("Port " + port + " busy, trying " + (port + 1)); port++; }
        }
        if (s == null) return;
        int P = port;

        s.createContext("/", ex -> sendHtml(ex, HTML)); // main page
        s.createContext("/api/stats", ex -> sendJson(ex, statsJson()));
        s.createContext("/api/rooms", ex -> { // list / add / delete
            try {
                if (ex.getRequestMethod().equals("GET")) sendJson(ex, roomsJson(q(ex, "search")));
                else if (ex.getRequestMethod().equals("POST")) {
                    Map<String, String> b = body(ex);
                    Room r = addRoom(b.get("roomNo"), b.getOrDefault("type", "Single"), Double.parseDouble(b.getOrDefault("rent", "0")));
                    sendJson(ex, "{\"ok\":true,\"room\":\"" + r.no + "\"}");
                } else {
                    Room r = findRoom(q(ex, "roomNo"));
                    if (r == null || r.filled() > 0) sendJson(ex, 400, "{\"error\":\"Cannot delete (not found or has tenants)\"}");
                    else { rooms.remove(r); save(); sendJson(ex, "{\"ok\":true}"); }
                }
            } catch (Exception e) { sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}"); }
        });
        // NEW: single room full detail (for click popup)
        s.createContext("/api/room", ex -> sendJson(ex, roomDetailJson(q(ex, "no"))));
        s.createContext("/api/tenants", ex -> {
            try {
                if (ex.getRequestMethod().equals("GET")) sendJson(ex, tenantsJson(q(ex, "search")));
                else if (ex.getRequestMethod().equals("POST")) {
                    Map<String, String> b = body(ex);
                    Tenant t = addTenant(b.get("name"), b.getOrDefault("phone", ""));
                    String rn = b.getOrDefault("roomNo", "").trim();
                    if (!rn.equals("")) try { allocate(t.id, rn); } catch (Exception e) { sendJson(ex, "{\"ok\":true,\"id\":\"" + t.id + "\",\"warn\":\"" + e.getMessage() + "\"}"); return; }
                    sendJson(ex, "{\"ok\":true,\"id\":\"" + t.id + "\"}");
                } else {
                    Tenant t = findTenant(q(ex, "id"));
                    if (t == null) sendJson(ex, 400, "{\"error\":\"Not found\"}");
                    else { if (!t.roomNo.equals("")) { Room r = findRoom(t.roomNo); if (r != null) r.members.remove(t.id); } tenants.remove(t); save(); sendJson(ex, "{\"ok\":true}"); }
                }
            } catch (Exception e) { sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}"); }
        });
        s.createContext("/api/allocate", ex -> {
            try { Map<String, String> b = body(ex); allocate(b.get("tenantId"), b.get("roomNo")); sendJson(ex, "{\"ok\":true}"); }
            catch (Exception e) { sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}"); }
        });
        s.createContext("/api/vacate", ex -> {
            try { vacate(body(ex).get("tenantId")); sendJson(ex, "{\"ok\":true}"); }
            catch (Exception e) { sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}"); }
        });
        s.createContext("/api/payments", ex -> {
            try {
                if (ex.getRequestMethod().equals("GET")) sendJson(ex, paymentsJson());
                else {
                    Map<String, String> b = body(ex);
                    Tenant t = findTenant(b.get("tenantId"));
                    if (t == null) throw new NotFound("Tenant not found");
                    double a = Double.parseDouble(b.get("amount"));
                    Payment p = new Payment("P" + String.format("%04d", pSeq++), t.id, a, b.get("month"));
                    payments.add(p); t.pay(a); save();
                    sendJson(ex, "{\"ok\":true}");
                }
            } catch (Exception e) { sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}"); }
        });
        s.createContext("/api/complaints", ex -> {
            try {
                if (ex.getRequestMethod().equals("GET")) sendJson(ex, complaintsJson());
                else {
                    Map<String, String> b = body(ex);
                    if (findTenant(b.get("tenantId")) == null) throw new NotFound("Tenant not found");
                    complaints.add(new Complaint("C" + String.format("%03d", cSeq++), b.get("tenantId"), b.get("text")));
                    save(); sendJson(ex, "{\"ok\":true}");
                }
            } catch (Exception e) { sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}"); }
        });
        s.createContext("/api/complaints/resolve", ex -> {
            try {
                for (Complaint c : complaints) if (c.id.equals(body(ex).get("id"))) { c.status = "Resolved"; save(); sendJson(ex, "{\"ok\":true}"); return; }
                sendJson(ex, 400, "{\"error\":\"Not found\"}");
            } catch (Exception e) { sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}"); }
        });
        // Reset demo data (fixes corrupt old files in one click)
        s.createContext("/api/reset", ex -> {
            try { java.nio.file.Files.createDirectories(java.nio.file.Paths.get("data")); seed(); sendJson(ex, "{\"ok\":true}"); }
            catch (Exception e) { sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}"); }
        });
        s.setExecutor(null); s.start();
        System.out.println("==========================================");
        System.out.println("  OPEN IN CHROME: http://localhost:" + P);
        System.out.println("==========================================");
    }

    // ----- small JSON helpers -----
    static String statsJson() {
        int occ = 0; for (Room r : rooms) occ += r.filled();
        int cap = 0; for (Room r : rooms) cap += r.capacity();
        double col = 0; for (Payment p : payments) col += p.amount;
        int pend = filter(complaints, c -> c.status.equals("Pending")).size();
        return "{\"rooms\":" + rooms.size() + ",\"beds\":" + cap + ",\"filled\":" + occ + ",\"vacant\":" + (cap - occ)
                + ",\"tenants\":" + tenants.size() + ",\"collected\":" + col + ",\"pending\":" + pend + "}";
    }
    static String roomsJson(String k) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        rooms.sort((a, b) -> a.no.compareTo(b.no));
        for (Room r : rooms) {
            if (k != null && !k.equals("") && !r.no.contains(k) && !r.type.toLowerCase().contains(k.toLowerCase())) continue;
            if (!first) sb.append(",");
            // occupant names + phones shown permanently (connected internally)
            StringBuilder names = new StringBuilder("[");
            boolean nf = true;
            for (String tid : r.members) {
                Tenant t = findTenant(tid); if (t == null) continue;
                if (!nf) names.append(",");
                names.append("\"").append(j(t.name)).append(" (").append(j(t.phone)).append(")\"");
                nf = false;
            }
            names.append("]");
            sb.append("{\"no\":\"" + r.no + "\",\"type\":\"" + r.type + "\",\"floor\":" + r.floor()
                    + ",\"rent\":" + r.rent + ",\"cap\":" + r.capacity() + ",\"filled\":" + r.filled()
                    + ",\"vacant\":" + r.vacant() + ",\"status\":\"" + r.status() + "\",\"names\":" + names + "}");
            first = false;
        }
        return sb + "]";
    }
    // detail for ONE room (click popup): tenants + rent paid? + complaints?
    static String j(String s) { // make safe for JSON
        if (s == null) return "";
        return s.replace("\\", "").replace("\"", "").replace("\n", " ");
    }
    static String roomDetailJson(String no) {
        Room r = findRoom(no);
        if (r == null) return "{\"error\":\"Room not found\"}";
        StringBuilder sb = new StringBuilder("{\"no\":\"" + r.no + "\",\"type\":\"" + r.type + "\",\"floor\":" + r.floor()
                + ",\"rent\":" + r.rent + ",\"cap\":" + r.capacity() + ",\"filled\":" + r.filled()
                + ",\"vacant\":" + r.vacant() + ",\"status\":\"" + r.status() + "\",\"tenants\":[");
        boolean first = true;
        for (String tid : r.members) {
            Tenant t = findTenant(tid); if (t == null) continue;
            // paid this month?
            boolean paid = false; double paidAmt = 0;
            for (Payment p : payments) if (p.tenantId.equals(tid) && p.month.equals(curMonth())) { paid = true; paidAmt += p.amount; }
            // active complaints (proper JSON array)
            StringBuilder cb = new StringBuilder("[");
            int cn = 0;
            for (Complaint c : complaints) if (c.tenantId.equals(tid) && c.status.equals("Pending")) {
                if (cn > 0) cb.append(",");
                cb.append("\"").append(j(c.text)).append("\""); cn++;
            }
            cb.append("]");
            if (!first) sb.append(",");
            sb.append("{\"id\":\"" + t.id + "\",\"name\":\"" + j(t.name) + "\",\"phone\":\"" + j(t.phone)
                    + "\",\"paid\":" + paid + ",\"paidAmt\":" + paidAmt + ",\"totalPaid\":" + t.totalPaid
                    + ",\"issues\":" + cb + ",\"complaintCount\":" + cn + "}");
            first = false;
        }
        sb.append("]}");
        return sb.toString();
    }
    static String tenantsJson(String k) {
        StringBuilder sb = new StringBuilder("[");
        boolean f = true;
        for (Tenant t : tenants) {
            if (k != null && !k.equals("") && !(t.name.toLowerCase().contains(k.toLowerCase()) || t.id.contains(k) || t.roomNo.contains(k))) continue;
            if (!f) sb.append(",");
            sb.append("{\"id\":\"" + t.id + "\",\"name\":\"" + t.name + "\",\"phone\":\"" + t.phone + "\",\"room\":\"" + t.roomNo + "\",\"paid\":" + t.totalPaid + "}");
            f = false;
        }
        return sb + "]";
    }
    static String paymentsJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < payments.size(); i++) {
            Payment p = payments.get(i); if (i > 0) sb.append(",");
            Tenant t = findTenant(p.tenantId);
            sb.append("{\"id\":\"" + p.id + "\",\"tenant\":\"" + (t == null ? p.tenantId : t.name) + "\",\"amt\":" + p.amount + ",\"month\":\"" + p.month + "\",\"date\":\"" + p.date + "\"}");
        }
        return sb + "]";
    }
    static String complaintsJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < complaints.size(); i++) {
            Complaint c = complaints.get(i); if (i > 0) sb.append(",");
            Tenant t = findTenant(c.tenantId);
            sb.append("{\"id\":\"" + c.id + "\",\"tenant\":\"" + (t == null ? c.tenantId : t.name) + "\",\"tid\":\"" + c.tenantId + "\",\"text\":\"" + c.text.replace("\"", "") + "\",\"status\":\"" + c.status + "\",\"date\":\"" + c.date + "\"}");
        }
        return sb + "]";
    }

    // ----- http helpers (do not need to explain) -----
    static void sendJson(HttpExchange ex, String j) throws IOException { sendJson(ex, 200, j); }
    static void sendJson(HttpExchange ex, int code, String j) throws IOException {
        byte[] b = j.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream o = ex.getResponseBody()) { o.write(b); }
    }
    static void sendHtml(HttpExchange ex, String h) throws IOException {
        byte[] b = h.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, b.length);
        try (OutputStream o = ex.getResponseBody()) { o.write(b); }
    }
    static String q(HttpExchange ex, String key) {
        String query = ex.getRequestURI().getRawQuery();
        if (query == null) return "";
        for (String p : query.split("&")) {
            String[] kv = p.split("=", 2);
            try { if (kv.length == 2 && URLDecoder.decode(kv[0], "UTF-8").equals(key)) return URLDecoder.decode(kv[1], "UTF-8"); }
            catch (Exception ignored) {}
        }
        return "";
    }
    static Map<String, String> body(HttpExchange ex) throws IOException {
        String s = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        Map<String, String> m = new HashMap<>();
        if (!s.startsWith("{")) return m;
        s = s.substring(1, s.length() - 1); // remove { }
        for (String part : s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
            String[] kv = part.split(":", 2);
            if (kv.length < 2) continue;
            m.put(kv[0].replace("\"", "").trim(), kv[1].replace("\"", "").trim());
        }
        return m;
    }

    // ================= WEBSITE =================
    static final String HTML = """
<!DOCTYPE html><html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>PGMate</title>
<style>*{box-sizing:border-box;font-family:Segoe UI,Arial}body{margin:0;background:#eef2f7}header{background:#1f2937;color:#fff;padding:16px 24px}nav{display:flex;gap:8px;flex-wrap:wrap;margin-top:10px}.tab{background:#374151;color:#fff;border:0;padding:9px 16px;border-radius:8px;cursor:pointer}.tab.on{background:#2563eb}main{padding:20px;max-width:1100px;margin:auto}.pg{display:none}.pg.on{display:block}.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:12px;margin-bottom:14px}.stat{background:#fff;border-radius:12px;padding:14px;text-align:center}.stat b{font-size:22px;display:block}.card{background:#fff;border-radius:12px;padding:16px;margin-bottom:14px}.form{display:flex;gap:8px;flex-wrap:wrap}input,select{padding:9px;border:1px solid #ccc;border-radius:8px;flex:1;min-width:130px}button{background:#2563eb;color:#fff;border:0;padding:9px 15px;border-radius:8px;cursor:pointer}.floor{margin:10px 0}.rooms{display:flex;gap:10px;flex-wrap:wrap}.room{width:120px;padding:12px;border-radius:10px;border:2px solid #ccc;cursor:pointer;text-align:center;background:#fff}.room small{display:block}.g{border-color:#16a34a;background:#dcfce7}.o{border-color:#d97706;background:#fef3c7}.r{border-color:#dc2626;background:#fee2e2}.room b{font-size:20px}table{width:100%;border-collapse:collapse}th,td{padding:8px;border-bottom:1px solid #eee;font-size:14px;text-align:left}#modal{display:none;position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,.5)}#box{background:#fff;max-width:600px;margin:60px auto;padding:20px;border-radius:12px;max-height:80vh;overflow:auto}.pill{padding:2px 10px;border-radius:12px;color:#fff;font-size:12px}.yes{background:#16a34a}.no{background:#dc2626}.ten{border:1px solid #eee;border-radius:8px;padding:10px;margin:8px 0}#toast{position:fixed;bottom:20px;right:20px;background:#111;color:#fff;padding:10px 16px;border-radius:8px;display:none}.hint{font-size:13px;color:#666}</style>
</head><body>
<header><h2 style="margin:0">PGMate</h2><div>Floor 1 = 101,102... | Floor 2 = 201,202... Click a room to see details</div>
<nav><button class="tab on" data-p="d">Dashboard</button><button class="tab" data-p="r">Rooms</button><button class="tab" data-p="t">Tenants</button><button class="tab" data-p="p">Payments</button><button class="tab" data-p="c">Complaints</button></nav></header>
<main>
<div id="p-d" class="pg on"><div class="cards" id="stats"></div><div class="card"><h3>Floor-wise Rooms (click room number) <button onclick="resetAll()" style="float:right;background:#666">Reset Demo</button></h3><div id="floors"></div></div></div>
<div id="p-r" class="pg"><div class="card"><h3>Add Room</h3><div class="form">
<input id="rn" placeholder="Room No: 101 (F1), 201 (F2)"><select id="rt"><option>Single</option><option>Double</option><option>Triple</option><option>Four</option></select>
<input id="rr" type="number" placeholder="Rent Rs"><button onclick="addRoom()">Add</button></div>
<p class="hint">Single=1 bed, Double=2 beds, Triple=3 beds, Four=4 beds. First digit = floor.</p></div>
<div class="card"><h3>All Rooms (tenants shown permanently)</h3><table><thead><tr><th>Room</th><th>Floor</th><th>Type</th><th>Beds</th><th>Status</th><th>Tenants (name - phone)</th><th></th></tr></thead><tbody id="roomT"></tbody></table></div></div>
<div id="p-t" class="pg"><div class="card"><h3>New Tenant</h3><div class="form">
<input id="tn" placeholder="Name"><input id="tp" placeholder="Phone"><input id="tr" placeholder="Room (optional e.g. 102)"><button onclick="addT()">Add</button></div></div>
<div class="card"><h3>Tenants (name + phone + room always visible)</h3><table><thead><tr><th>ID</th><th>Name</th><th>Phone</th><th>Room</th><th>Total Paid</th><th></th></tr></thead><tbody id="tenT"></tbody></table></div>
<div class="card"><h3>Allocate / Vacate</h3><div class="form"><input id="aT" placeholder="Tenant ID"><input id="aR" placeholder="Room No"><button onclick="al()">Allocate</button><button onclick="va()">Vacate</button></div></div></div>
<div id="p-p" class="pg"><div class="card"><h3>Record Rent</h3><div class="form">
<input id="pT" placeholder="Tenant ID"><input id="pA" type="number" placeholder="Amount"><input id="pM" type="month"><button onclick="addP()">Save</button></div></div>
<div class="card"><h3>Payments</h3><table><thead><tr><th>ID</th><th>Tenant</th><th>Amt</th><th>Month</th></tr></thead><tbody id="payT"></tbody></table></div></div>
<div id="p-c" class="pg"><div class="card"><h3>New Complaint</h3><div class="form">
<input id="cT" placeholder="Tenant ID"><input id="cX" placeholder="Issue" style="flex:2"><button onclick="addC()">Add</button></div></div>
<div class="card"><h3>Complaints</h3><table><thead><tr><th>ID</th><th>Tenant</th><th>Issue</th><th>Status</th><th></th></tr></thead><tbody id="comT"></tbody></table></div></div>
</main>
<div id="modal"><div id="box"><button onclick="closeM()" style="float:right">X</button><div id="detail"></div></div></div>
<div id="toast"></div>
<script>
document.querySelectorAll('.tab').forEach(b=>b.onclick=()=>{document.querySelectorAll('.tab').forEach(x=>x.classList.remove('on'));document.querySelectorAll('.pg').forEach(x=>x.classList.remove('on'));b.classList.add('on');document.getElementById('p-'+b.dataset.p).classList.add('on')});
document.getElementById('pM').value=new Date().toISOString().slice(0,7);
function T(m,e){var t=document.getElementById('toast');t.textContent=m;t.style.display='block';t.style.background=e?'#b91c1c':'#111';setTimeout(()=>t.style.display='none',2200)}
async function api(u,mt,d){var o={method:mt||'GET'};if(d){o.headers={'Content-Type':'application/json'};o.body=JSON.stringify(d)}var r=await fetch(u,o);var j=await r.json().catch(()=>({}));if(!r.ok)throw new Error(j.error||r.status);return j}
var V=id=>document.getElementById(id).value.trim();
async function refresh(){
var s=await api('/api/stats');
document.getElementById('stats').innerHTML='<div class=stat><b>'+s.rooms+'</b>Rooms</div><div class=stat><b>'+s.beds+'</b>Beds</div><div class=stat><b>'+s.filled+'</b>Filled</div><div class=stat><b>'+s.vacant+'</b>Vacant</div><div class=stat><b>'+s.tenants+'</b>Tenants</div><div class=stat><b>Rs.'+s.collected+'</b>Collected</div><div class=stat><b>'+s.pending+'</b>Pending</div>';
var rooms=await api('/api/rooms');
var fl={};rooms.forEach(r=>{(fl[r.floor]=fl[r.floor]||[]).push(r)});
var h='';Object.keys(fl).sort().forEach(f=>{h+='<div class=floor><b>Floor '+f+'</b><div class=rooms>';fl[f].forEach(r=>{var c=r.status=='Available'?'g':(r.status=='Partial'?'o':'r');var who=(r.names&&r.names.length?r.names.join('<br>'):'<i>Empty</i>');h+='<div class="room '+c+'" onclick="openRoom(\\''+r.no+'\\')" style="width:150px"><b>'+r.no+'</b><small>'+r.type+' ('+r.filled+'/'+r.cap+')</small><small>'+r.status+' | '+r.vacant+' vacant</small><small>'+who+'</small></div>'});h+='</div></div>'});
document.getElementById('floors').innerHTML=h||'No rooms';
document.getElementById('roomT').innerHTML=rooms.map(r=>'<tr><td><a href="#" onclick="openRoom(\\''+r.no+'\\');return false"><b>'+r.no+'</b></a></td><td>'+r.floor+'</td><td>'+r.type+'</td><td>'+r.filled+'/'+r.cap+'</td><td>'+r.status+'</td><td><small>'+((r.names&&r.names.length)?r.names.join('<br>'):'-')+'</small></td><td><button onclick="delR(\\''+r.no+'\\')">Del</button></td></tr>').join('');
var tl=await api('/api/tenants');
document.getElementById('tenT').innerHTML=tl.map(t=>'<tr><td>'+t.id+'</td><td><b>'+t.name+'</b></td><td>'+t.phone+'</td><td>'+(t.room||'None')+'</td><td>Rs.'+t.paid+'</td><td><button onclick="delT(\\''+t.id+'\\')">Remove</button></td></tr>').join('')||'<tr><td colspan=6><i>No tenants - click Reset Demo on Dashboard if rooms show filled</i></td></tr>';
var pl=await api('/api/payments');
document.getElementById('payT').innerHTML=[...pl].reverse().map(p=>'<tr><td>'+p.id+'</td><td>'+p.tenant+'</td><td>Rs.'+p.amt+'</td><td>'+p.month+'</td></tr>').join('');
var cl=await api('/api/complaints');
document.getElementById('comT').innerHTML=[...cl].reverse().map(c=>'<tr><td>'+c.id+'</td><td>'+c.tenant+'</td><td>'+c.text+'</td><td>'+c.status+'</td><td>'+(c.status=='Pending'?'<button onclick="resC(\\''+c.id+'\\')">Done</button>':'')+'</td></tr>').join('');
}
async function openRoom(no){
var d=await api('/api/room?no='+no);
if(d.error){T(d.error,1);return}
var h='<h2>Room '+d.no+' ('+d.type+')</h2><p>Floor: '+d.floor+' | Rent: Rs.'+d.rent+' | Beds: '+d.filled+'/'+d.cap+' | Vacant: '+d.vacant+' | <b>'+d.status+'</b></p>';
if(d.cap>1){h+='<p>'+(d.status=='Full'?'Fully filled':(d.status=='Partial'?'Partially filled - '+d.vacant+' bed(s) empty':'All '+d.cap+' beds empty'))+'</p>'}
if(d.tenants.length==0)h+='<p><i>No tenants yet</i></p>';
d.tenants.forEach(t=>{
var pay=t.paid?'<span class="pill yes">Rent Paid Rs.'+t.paidAmt+' (this month)</span>':'<span class="pill no">Rent NOT Paid (this month)</span>';
var comp=t.complaintCount>0?'<span class="pill no">'+t.complaintCount+' active complaint(s)</span>':'<span class="pill yes">No complaints</span>';
h+='<div class=ten><b>'+t.name+' ('+t.id+')</b><br>Phone: '+t.phone+'<br>'+pay+' '+comp+'<br>Total paid: Rs.'+t.totalPaid+'<br>Complaints: '+(t.issues&&t.issues.length?t.issues.join(', '):'None')+'</div>'});
document.getElementById('detail').innerHTML=h;
document.getElementById('modal').style.display='block';
}
function closeM(){document.getElementById('modal').style.display='none'}
async function addRoom(){try{await api('/api/rooms','POST',{roomNo:V('rn'),type:V('rt'),rent:V('rr')});T('Room added');refresh()}catch(e){T(e.message,1)}}
async function delR(n){if(!confirm('Delete '+n+'?'))return;var r=await fetch('/api/rooms?roomNo='+n,{method:'DELETE'});if(!r.ok){T((await r.json()).error,1);return}T('Deleted');refresh()}
async function addT(){try{var j=await api('/api/tenants','POST',{name:V('tn'),phone:V('tp'),roomNo:V('tr')});T('Added '+j.id+(j.warn?' ('+j.warn+')':''));refresh()}catch(e){T(e.message,1)}}
async function delT(id){if(!confirm('Remove '+id+'?'))return;var r=await fetch('/api/tenants?id='+id,{method:'DELETE'});if(!r.ok){T((await r.json()).error,1);return}T('Removed');refresh()}
async function al(){try{await api('/api/allocate','POST',{tenantId:V('aT'),roomNo:V('aR')});T('Allocated');refresh()}catch(e){T(e.message,1)}}
async function va(){try{await api('/api/vacate','POST',{tenantId:V('aT')});T('Vacated');refresh()}catch(e){T(e.message,1)}}
async function addP(){try{await api('/api/payments','POST',{tenantId:V('pT'),amount:V('pA'),month:V('pM')});T('Saved');refresh()}catch(e){T(e.message,1)}}
async function addC(){try{await api('/api/complaints','POST',{tenantId:V('cT'),text:V('cX')});T('Added');refresh()}catch(e){T(e.message,1)}}
async function resC(id){await api('/api/complaints/resolve','POST',{id});T('Resolved');refresh()}
async function resetAll(){if(!confirm('Reset to demo data?'))return;await api('/api/reset','POST',{});T('Reset done');refresh()}
refresh();
</script></body></html>
        """;
}
