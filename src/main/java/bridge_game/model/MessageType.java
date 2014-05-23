package bridge_game.model;

/**
 * Typy wiadomości wysyłanych pomiędzy klientem
 * a serwerem (Tokeny dołącząne są na początku
 * wiadomości
 */
public enum MessageType {
    /**
     * Wiadomość wysyłana do klienta
    */
    MESSAGE,

    /**
     * Serwer wysyła do gracza informację powitalną
     */
    WELCOME,

    /**
     * Gracz próbuje wykonać ruch (zalicytować)
     */
    MOVE,

    /**
     * Stół jest już pełny, więc nie można się podłączyć
     */
    FUll,

    /**
     * Koniec rozgrywki
     */
    QUIT
}
