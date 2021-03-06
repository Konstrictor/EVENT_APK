package bi.konstrictor.ikirori;

import org.json.JSONException;
import org.json.JSONObject;

public class Profile {
    String fullname, avatar, phone, email, date, autres, qr, id;
    Ticket ticket;

    public Profile(String id, String fullname, String avatar, String phone, String email, String date, String autres, String qr, JSONObject json_ticket) throws JSONException {
        this.id = id;
        this.fullname = fullname;
        this.avatar = avatar;
        this.phone = phone;
        this.email = email;
        this.date = date;
        this.autres = autres;
        this.qr = qr;
        this.ticket = new Ticket(
                json_ticket.getString("id"),
                json_ticket.getString("name"),
                json_ticket.getString("autres"),
                json_ticket.getDouble("price"),
                json_ticket.getDouble("consommable")
        );
    }
}
