package lib;

import java.util.ArrayList;
import java.util.List;

public class UsersDatabase {
    public static List<Users> getAllUsers() {
        List<Users> users = new ArrayList<>();

        //initial database
        users.add(new Users(1, "Ana", 22, "São Paulo"));
        users.add(new Users(2, "João", 35, "Rio de Janeiro"));
        users.add(new Users(3, "Maria", 28, "Belo Horizonte"));
        users.add(new Users(4, "Pedro", 41, "Curitiba"));
        users.add(new Users(5, "Carla", 33, "Salvador"));
        users.add(new Users(6, "Lucas", 25, "Porto Alegre"));
        users.add(new Users(7, "Fernanda", 47, "Recife"));
        users.add(new Users(8, "Rafael", 31, "Fortaleza"));
        users.add(new Users(9, "Juliana", 29, "Brasília"));
        users.add(new Users(10, "Marcos", 38, "Manaus"));
        users.add(new Users(11, "Patrícia", 26, "São Paulo"));
        users.add(new Users(12, "Bruno", 44, "Rio de Janeiro"));
        users.add(new Users(13, "Amanda", 23, "Belo Horizonte"));
        users.add(new Users(14, "Felipe", 39, "Curitiba"));
        users.add(new Users(15, "Gabriela", 32, "Salvador"));
        users.add(new Users(16, "Eduardo", 36, "Porto Alegre"));
        users.add(new Users(17, "Letícia", 27, "Recife"));
        users.add(new Users(18, "Ricardo", 42, "Fortaleza"));
        users.add(new Users(19, "Camila", 30, "Brasília"));
        users.add(new Users(20, "Daniel", 45, "Manaus"));
        users.add(new Users(21, "Vanessa", 34, "São Paulo"));
        users.add(new Users(22, "Thiago", 37, "Rio de Janeiro"));
        users.add(new Users(23, "Isabela", 40, "Belo Horizonte"));
        users.add(new Users(24, "Gustavo", 29, "Curitiba"));
        users.add(new Users(25, "Larissa", 31, "Salvador"));
        users.add(new Users(26, "Rodrigo", 43, "Porto Alegre"));
        users.add(new Users(27, "Natália", 26, "Recife"));
        users.add(new Users(28, "André", 38, "Fortaleza"));
        users.add(new Users(29, "Bianca", 35, "Brasília"));
        users.add(new Users(30, "Vinicius", 33, "Manaus"));

        return users;
    }

    public static void clear() {
        getAllUsers().clear();
    }

}
