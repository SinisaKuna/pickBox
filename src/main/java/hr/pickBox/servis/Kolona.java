package hr.pickBox.servis;

public class Kolona {
    private String naziv;
    private String polje;
    private String id;

    public Kolona(String naziv, String polje, String id) {
        this.naziv = naziv;
        this.polje = polje;
        this.id = id;
    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public String getPolje() {
        return polje;
    }

    public void setPolje(String polje) {
        this.polje = polje;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

