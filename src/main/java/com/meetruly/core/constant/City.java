package com.meetruly.core.constant;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum City {

    SOFIA("Sofia", Country.BULGARIA),
    PLOVDIV("Plovdiv", Country.BULGARIA),
    VARNA("Varna", Country.BULGARIA),
    BURGAS("Burgas", Country.BULGARIA),
    RUSE("Ruse", Country.BULGARIA),
    STARA_ZAGORA("Stara Zagora", Country.BULGARIA),
    PLEVEN("Pleven", Country.BULGARIA),
    SLIVEN("Sliven", Country.BULGARIA),
    DOBRICH("Dobrich", Country.BULGARIA),
    SHUMEN("Shumen", Country.BULGARIA),

    BERLIN("Berlin", Country.GERMANY),
    MUNICH("Munich", Country.GERMANY),
    HAMBURG("Hamburg", Country.GERMANY),
    FRANKFURT("Frankfurt", Country.GERMANY),
    COLOGNE("Cologne", Country.GERMANY),

    PARIS("Paris", Country.FRANCE),
    MARSEILLE("Marseille", Country.FRANCE),
    LYON("Lyon", Country.FRANCE),
    TOULOUSE("Toulouse", Country.FRANCE),
    NICE("Nice", Country.FRANCE),

    MADRID("Madrid", Country.SPAIN),
    BARCELONA("Barcelona", Country.SPAIN),
    VALENCIA("Valencia", Country.SPAIN),
    SEVILLE("Seville", Country.SPAIN),
    MALAGA("Malaga", Country.SPAIN),

    LONDON("London", Country.UNITED_KINGDOM),
    MANCHESTER("Manchester", Country.UNITED_KINGDOM),
    LIVERPOOL("Liverpool", Country.UNITED_KINGDOM),
    BIRMINGHAM("Birmingham", Country.UNITED_KINGDOM),
    LEEDS("Leeds", Country.UNITED_KINGDOM);

    private final String displayName;
    private final Country country;

    City(String displayName, Country country) {
        this.displayName = displayName;
        this.country = country;
    }
    public static List<City> getCitiesByCountry(Country country) {
        return Arrays.stream(City.values())
                .filter(city -> city.getCountry() == country)
                .collect(Collectors.toList());
    }
}
