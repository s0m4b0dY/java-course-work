package com.voronina.course.randomuserapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voronina.course.ApiObject;

public class RandomUser implements ApiObject {
  static final String[] CSV_HEADERS = {
      "gender",
      "title",
      "first",
      "last",
      "city",
      "streetName",
      "streetNumber",
      "email",
      "uuid",
      "username",
      "password",
      "registeredDate",
      "registeredAge",
      "phone",
      "cell",
      "idName",
      "idValue"
  };

  private String gender;
  private Name name;
  private Location location;
  private String email;
  private Login login;
  private Registered registered;
  private String phone;
  private String cell;
  private Id id;

  @Override
  public Gson toGson() {
    return new GsonBuilder().serializeNulls().create();
  }

  @Override
  public String[] toCsvFields() {
    String title = name != null ? safe(name.getTitle()) : "";
    String first = name != null ? safe(name.getFirst()) : "";
    String last = name != null ? safe(name.getLast()) : "";

    String city = location != null ? safe(location.getCity()) : "";
    String streetName = location != null && location.getStreet() != null ? safe(location.getStreet().getName()) : "";
    String streetNumber = location != null && location.getStreet() != null
        ? String.valueOf(location.getStreet().getNumber())
        : "";

    String uuid = login != null ? safe(login.getUuid()) : "";
    String username = login != null ? safe(login.getUsername()) : "";
    String password = login != null ? safe(login.getPassword()) : "";

    String registeredDate = registered != null ? safe(registered.getDate()) : "";
    String registeredAge = registered != null ? String.valueOf(registered.getAge()) : "";

    String idName = id != null ? safe(id.getName()) : "";
    String idValue = id != null ? safe(id.getValue()) : "";

    return new String[] {
        safe(gender),
        title,
        first,
        last,
        city,
        streetName,
        streetNumber,
        safe(email),
        uuid,
        username,
        password,
        registeredDate,
        registeredAge,
        safe(phone),
        safe(cell),
        idName,
        idValue
    };
  }

  @Override
  public String[] csvHeaders() {
    return CSV_HEADERS;
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public Name getName() {
    return name;
  }

  public void setName(Name name) {
    this.name = name;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Login getLogin() {
    return login;
  }

  public void setLogin(Login login) {
    this.login = login;
  }

  public Registered getRegistered() {
    return registered;
  }

  public void setRegistered(Registered registered) {
    this.registered = registered;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getCell() {
    return cell;
  }

  public void setCell(String cell) {
    this.cell = cell;
  }

  public Id getId() {
    return id;
  }

  public void setId(Id id) {
    this.id = id;
  }

  public static class Name {
    private String title;
    private String first;
    private String last;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getFirst() {
      return first;
    }

    public void setFirst(String first) {
      this.first = first;
    }

    public String getLast() {
      return last;
    }

    public void setLast(String last) {
      this.last = last;
    }
  }

  public static class Location {
    private Street street;
    private String city;

    public Street getStreet() {
      return street;
    }

    public void setStreet(Street street) {
      this.street = street;
    }

    public String getCity() {
      return city;
    }

    public void setCity(String city) {
      this.city = city;
    }
  }

  public static class Street {
    private int number;
    private String name;

    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class Login {
    private String uuid;
    private String username;
    private String password;

    public String getUuid() {
      return uuid;
    }

    public void setUuid(String uuid) {
      this.uuid = uuid;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }

  public static class Registered {
    private String date;
    private int age;

    public String getDate() {
      return date;
    }

    public void setDate(String date) {
      this.date = date;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }
  }

  public static class Id {
    private String name;
    private String value;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
