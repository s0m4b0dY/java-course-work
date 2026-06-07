package com.voronina.course.randomuserapi;

import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUserTest {
  @Test
  void csvFieldsUseNestedObjects() {
    String json = """
        {
          "gender":"female",
          "name":{"title":"Ms","first":"Ann","last":"Smith"},
          "location":{"city":"Berlin","street":{"number":12,"name":"Main"}},
          "email":"ann@example.com",
          "login":{"uuid":"u1","username":"ann","password":"pw"},
          "registered":{"date":"2020-01-01","age":4},
          "phone":"123",
          "cell":"456",
          "id":{"name":"ID","value":"999"}
        }
        """;

    RandomUser user = new GsonBuilder().create().fromJson(json, RandomUser.class);
    String[] fields = user.toCsvFields();

    assertEquals("female", fields[0]);
    assertEquals("Ms", fields[1]);
    assertEquals("Ann", fields[2]);
    assertEquals("Smith", fields[3]);
    assertEquals("Berlin", fields[4]);
    assertEquals("Main", fields[5]);
    assertEquals("12", fields[6]);
    assertEquals("ann@example.com", fields[7]);
    assertEquals("u1", fields[8]);
    assertEquals("ann", fields[9]);
    assertEquals("pw", fields[10]);
    assertEquals("2020-01-01", fields[11]);
    assertEquals("4", fields[12]);
    assertEquals("123", fields[13]);
    assertEquals("456", fields[14]);
    assertEquals("ID", fields[15]);
    assertEquals("999", fields[16]);
    assertArrayEquals(RandomUser.CSV_HEADERS, user.csvHeaders());
    assertNotNull(user.toGson());
  }

  @Test
  void settersAndGettersWork() {
    RandomUser user = new RandomUser();
    RandomUser.Name name = new RandomUser.Name();
    name.setTitle("Mr");
    name.setFirst("Bob");
    name.setLast("Stone");

    RandomUser.Street street = new RandomUser.Street();
    street.setNumber(7);
    street.setName("Oak");

    RandomUser.Location location = new RandomUser.Location();
    location.setCity("Paris");
    location.setStreet(street);

    RandomUser.Login login = new RandomUser.Login();
    login.setUuid("uuid");
    login.setUsername("bob");
    login.setPassword("pw");

    RandomUser.Registered registered = new RandomUser.Registered();
    registered.setAge(10);
    registered.setDate("date");

    RandomUser.Id id = new RandomUser.Id();
    id.setName("pass");
    id.setValue("val");

    user.setGender("male");
    user.setName(name);
    user.setLocation(location);
    user.setEmail("e");
    user.setLogin(login);
    user.setRegistered(registered);
    user.setPhone("p");
    user.setCell("c");
    user.setId(id);

    assertEquals("male", user.getGender());
    assertEquals("Mr", user.getName().getTitle());
    assertEquals("Bob", user.getName().getFirst());
    assertEquals("Stone", user.getName().getLast());
    assertEquals("Paris", user.getLocation().getCity());
    assertEquals("Oak", user.getLocation().getStreet().getName());
    assertEquals(7, user.getLocation().getStreet().getNumber());
    assertEquals("e", user.getEmail());
    assertEquals("uuid", user.getLogin().getUuid());
    assertEquals("bob", user.getLogin().getUsername());
    assertEquals("pw", user.getLogin().getPassword());
    assertEquals("date", user.getRegistered().getDate());
    assertEquals(10, user.getRegistered().getAge());
    assertEquals("p", user.getPhone());
    assertEquals("c", user.getCell());
    assertEquals("pass", user.getId().getName());
    assertEquals("val", user.getId().getValue());
  }

  @Test
  void nullNestedObjectsBecomeEmptyCsvFields() {
    RandomUser user = new RandomUser();
    String[] fields = user.toCsvFields();

    for (String field : fields) {
      assertEquals("", field);
    }
  }
}
