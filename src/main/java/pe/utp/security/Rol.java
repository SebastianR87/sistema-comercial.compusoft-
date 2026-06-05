package pe.utp.security;

public enum Rol {
    ADMINISTRADOR("Administrador"),
    VENDEDOR("Vendedor"),
    ALMACENERO("Almacenero");

    private final String cargo;

    Rol(String cargo) {
        this.cargo = cargo;
    }

    public String getCargo() {
        return cargo;
    }

    public static Rol desdeCargo(String cargo) {
        if (cargo == null) return null;
        for (Rol rol : values()) {
            if (rol.cargo.equalsIgnoreCase(cargo.trim())) {
                return rol;
            }
        }
        return null;
    }
}
