package com.company;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public class Main {

    private Connection connect() {
        // SQLite connection string
        String url = "jdbc:sqlite:subject-dept.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public void insert(String table, String field1, String field2, String value1, String value2) {
        String sql = String.format("INSERT INTO %s(%s,%s) VALUES(?,?)",table,field1,field2,value1,value2);

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1,value1);
            pstmt.setString(2,value2);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void dropTables(){
        try{
            Connection conn = this.connect();
            PreparedStatement pstmtDS = conn.prepareStatement("DROP TABLE Subjects");
            pstmtDS.executeUpdate();
            PreparedStatement pstmtDD = conn.prepareStatement("DROP TABLE Departments");
            pstmtDD.executeUpdate();
        } catch(SQLException e){
                System.out.println(e.getMessage());
        }
    }

    public void createTables(){
        try{
            Connection conn = this.connect();
            PreparedStatement pstmtCS = conn.prepareStatement(
                    "CREATE TABLE `Subjects` ( `SUBJECT_CODE` TEXT UNIQUE, `SUBJECT_NAME` TEXT, " +
                            "PRIMARY KEY(`SUBJECT_CODE`) )");
            pstmtCS.executeUpdate();
            PreparedStatement pstmtCD = conn.prepareStatement(
                    "CREATE TABLE `Departments` ( `DEPARTMENT_CODE` TEXT UNIQUE, `DEPARTMENT_NAME` TEXT, " +
                            "PRIMARY KEY(`DEPARTMENT_CODE`) )");
            pstmtCD.executeUpdate();
        } catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void loadDB(){
        try {
            Document doc = Jsoup.connect("https://aps2.missouriwestern.edu/schedule/Default.asp?tck=201910").get();
            Elements selects = doc.select("select");
            selects.remove(0);
            for (int i = 0; i<selects.size();i++) {
                Elements options = selects.get(i).select("option");
                for(int j =1;j<options.size();j++){
                    if(i==0)
                        insert("Subjects","SUBJECT_CODE","SUBJECT_NAME",
                                options.get(j).val(),options.get(j).text());
                    if(i==1)
                        insert("Departments","DEPARTMENT_CODE","DEPARTMENT_NAME",
                                options.get(j).val(),options.get(j).text());
                    System.out.println(options.get(j));
                }
                System.out.println("-----------------");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void postCourse(){
        try {
            String crn;
            String course;
            String title;
            String section;
            int people;
            int seatsAvailable;
            ArrayList<Course> courses = new ArrayList<>();

            Connection conn = this.connect();
            String statement = "SELECT * FROM Departments";
            ResultSet rs = conn.createStatement().executeQuery(statement);
            while(rs.next()){
                String dCode = rs.getString("DEPARTMENT_CODE");
                Document doc = Jsoup.connect("https://aps2.missouriwestern.edu/schedule/Default.asp?tck=201910")
                        .data("course_number","")
                        .data("subject","ALL")
                        .data("department",dCode)
                        .data("display_closed","YES")
                        .data("course_type","ALL").post();
                Elements listrows = doc.select("table.results tr.list_row");
                for (Element row:listrows) {
                    Elements tds = row.select("td");
                    crn = tds.get(0).text();
                    course = tds.get(1).text();
                    section = tds.get(2).text();
                    title = tds.get(4).text();
                    Course courseObject = new Course(crn,course,section,title);
                    courses.add(courseObject);
                }
                Elements detailrows = doc.select("table.results tr.detail_row");
                for (int i =0;i<detailrows.size();i++) {
                    Elements seats = detailrows.get(i).select("span.course_seats");
                    parseSeatText(courses.get(i),seats.get(0));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void parseSeatText(Course course, Element seat){
        String seatText = seat.text();
        String[] seatSplit = seatText.split("Section");
        int maxEnroll = Integer.parseInt(seatSplit[0].replaceAll("\\D", ""));
        int seatsAvail = Integer.parseInt(seatSplit[1].replaceAll("\\D", ""));
        course.setSeatsAvailable(seatsAvail);
        course.setPeople(maxEnroll-seatsAvail);
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.dropTables();
        app.createTables();
        app.loadDB();
        app.postCourse();
    }

    class Course{
        String crn;
        String course;
        String title;
        String section;
        int people;
        int seatsAvailable;

        public Course(String crn, String course, String title, String section) {
            this.crn = crn;
            this.course = course;
            this.title = title;
            this.section = section;
        }

        public String getCRN() {
            return crn;
        }

        public void setCRN(String crn) {
            this.crn = crn;
        }

        public String getCourse() {
            return course;
        }

        public void setCourse(String course) {
            this.course = course;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public int getPeople() {
            return people;
        }

        public void setPeople(int people) {
            this.people = people;
        }

        public int getSeatsAvailable() {
            return seatsAvailable;
        }

        public void setSeatsAvailable(int seatsAvailable) {
            this.seatsAvailable = seatsAvailable;
        }
    }
}
