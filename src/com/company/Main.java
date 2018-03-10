package com.company;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public class Main {
    //To Do:
    //report stuff.

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
        String sql = String.format("INSERT INTO %s(%s,%s) VALUES(?,?)", table, field1, field2, value1, value2);

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, value1);
            pstmt.setString(2, value2);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void insertSection(String crn, String courseID, String department, String discipline, String courseNumber,
                              int sectionNumber, String lectureType, String title, int hours, String days, String time,
                              String room, String instructor, int maximumEnrollment, int seatsAvailable, String message,
                              String term, String beginDate, String endDate, String url) {
        //VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        //(%s,%s,%s,%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%d,%d,%s,%s,%s,%s,%s)
        String sql = String.format("INSERT INTO Sections" +
                        " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", crn, courseID, department,
                discipline, courseNumber, sectionNumber, lectureType, title, hours, days,
                time, room, instructor, maximumEnrollment, seatsAvailable, message, term,
                beginDate, endDate, url);
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, crn);
            pstmt.setString(2, courseID);
            pstmt.setString(3, department);
            pstmt.setString(4, discipline);
            pstmt.setString(5, courseNumber);
            pstmt.setInt(6, sectionNumber);
            pstmt.setString(7, lectureType);
            pstmt.setString(8, title);
            pstmt.setInt(9, hours);
            pstmt.setString(10, days);
            pstmt.setString(11, time);
            pstmt.setString(12, room);
            pstmt.setString(13, instructor);
            pstmt.setInt(14, maximumEnrollment);
            pstmt.setInt(15, seatsAvailable);
            pstmt.setString(16, message);
            pstmt.setString(17, term);
            pstmt.setString(18, beginDate);
            pstmt.setString(19, endDate);
            pstmt.setString(20, url);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }


    }

    public void dropTables() {
        try {
            Connection conn = this.connect();
            PreparedStatement pstmtDS = conn.prepareStatement("DROP TABLE Subjects");
            pstmtDS.executeUpdate();
            PreparedStatement pstmtDD = conn.prepareStatement("DROP TABLE Departments");
            pstmtDD.executeUpdate();
            PreparedStatement pstmtDSec = conn.prepareStatement("DROP TABLE Sections");
            pstmtDSec.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void dropSingleDepartment(String targetDept) {
        try {
            Connection conn = this.connect();
            PreparedStatement pstmtDTD = conn.prepareStatement("DELETE FROM Sections " +
                    "WHERE DEPARTMENT_NAME = " + targetDept);
            pstmtDTD.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void restoreSingleDepartment(String targetDeptCode) {
        try {
            int hours;
            int maximumEnrollment = 0;
            int seatsAvailable = 0;
            int sectionNumber;
            String url;
            String courseID;
            String department;
            String crn;
            String discipline;
            String lectureType;
            String courseNumber;
            String endDate = "";
            String beginDate = "";
            String term = "";
            String room = "";
            String message = "";
            String days = "";
            String time = "";
            String instructor;
            String title;
            ArrayList<Section> sections = new ArrayList<>();

            Connection conn = this.connect();
            //this SQL statement is different from the postSections() method
            String statement = "SELECT * FROM Departments WHERE DEPARTMENT_CODE LIKE " + targetDeptCode.toUpperCase();
            ResultSet rs = conn.createStatement().executeQuery(statement);
            while (rs.next()) {
                String dCode = rs.getString("DEPARTMENT_CODE");
                Document doc = Jsoup.connect("https://aps2.missouriwestern.edu/schedule/Default.asp?tck=201910")
                        .data("course_number", "")
                        .data("subject", "ALL")
                        .data("department", dCode)
                        .data("display_closed", "YES")
                        .data("course_type", "ALL")
                        .timeout(100 * 1000).post();
                Elements listrows = doc.select("table.results tr.list_row");
                if (listrows.size() > 0) {

                    for (Element row : listrows) {
                        Elements tds = row.select("td");
                        Elements urls = listrows.select("td a[href]");

                        int tdsCount = tds.size();

                        switch (tdsCount) {
                            case 10:
                                department = dCode;
                                crn = tds.get(0).text().trim();
                                url = urls.get(1).attr("href");
                                String s = tds.get(1).text().trim(); //getting courseID, discipline, and courseNumber from s
                                courseID = s;
                                discipline = s.substring(0, 3);//tr
                                courseNumber = s.substring(3, 6);
                                sectionNumber = Integer.parseInt(tds.get(2).text().trim());
                                lectureType = tds.get(3).text().trim();
                                title = tds.get(4).text().trim();
                                hours = Integer.parseInt(tds.get(5).text().trim());
                                days = tds.get(6).text().trim();
                                time = tds.get(7).text().trim();
                                room = tds.get(8).text().trim();
                                instructor = tds.get(9).text().trim();

                                Section sectionObject = new Section(crn, courseID, department, discipline, courseNumber, sectionNumber,
                                        lectureType, title, hours, days, time, room, instructor, maximumEnrollment, seatsAvailable, message,
                                        term, beginDate, endDate, url);
                                sections.add(sectionObject);
                                break;
                            case 5:
                                days += " | " + tds.get(1).text().trim();
                                time += " | " + tds.get(2).text().trim();
                                room += " | " + tds.get(3).text().trim();
                                break;
                        }


                    }
                    Elements detailrows = doc.select("table.results tr.detail_row");
                    for (int i = 0; i < detailrows.size(); i++) {
                        Elements seats = detailrows.get(i).select("span.course_seats");
                        parseSeatText(sections.get(i), seats.get(0));

                        Elements messages = detailrows.get(i).select("span.course_messages");
                        parseMessageText(sections.get(i), messages.get(0));

                        Elements terms = detailrows.get(i).select("span.course_term");
                        parseTermText(sections.get(i), terms.get(0));

                        Elements beginDates = detailrows.get(i).select("span.course_begins");
                        parseBeginDateText(sections.get(i), beginDates.get(0));

                        Elements endDates = detailrows.get(i).select("span.course_ends");
                        parseEndDateText(sections.get(i), endDates.get(0));
                    }
                }
            }//end of while(rs.next())
            for (int i = 0; i < sections.size(); i++) {
                insertSection(sections.get(i).crn,
                        sections.get(i).courseID,
                        sections.get(i).department,
                        sections.get(i).discipline,
                        sections.get(i).courseNumber,
                        sections.get(i).sectionNumber,
                        sections.get(i).lectureType,
                        sections.get(i).title,
                        sections.get(i).hours,
                        sections.get(i).days,
                        sections.get(i).time,
                        sections.get(i).room,
                        sections.get(i).instructor,
                        sections.get(i).maximumEnrollment,
                        sections.get(i).seatsAvailable,
                        sections.get(i).message,
                        sections.get(i).term,
                        sections.get(i).beginDate,
                        sections.get(i).endDate,
                        sections.get(i).url);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createTables() {
        try {
            Connection conn = this.connect();
            PreparedStatement pstmtCS = conn.prepareStatement(
                    "CREATE TABLE `Subjects` ( `SUBJECT_CODE` TEXT UNIQUE, `SUBJECT_NAME` TEXT, " +
                            "PRIMARY KEY(`SUBJECT_CODE`) )");
            pstmtCS.executeUpdate();
            PreparedStatement pstmtCD = conn.prepareStatement(
                    "CREATE TABLE `Departments` ( `DEPARTMENT_CODE` TEXT UNIQUE, `DEPARTMENT_NAME` TEXT, " +
                            "PRIMARY KEY(`DEPARTMENT_CODE`) )");
            pstmtCD.executeUpdate();

            PreparedStatement pstmtCsec = conn.prepareStatement("CREATE TABLE `Sections` ( `CRN` VARCHAR(10) UNIQUE, " +
                    "`COURSE_ID` VARCHAR(10), `DEPARTMENT` VARCHAR(10), `DISCIPLINE` VARCHAR(10), `COURSE_NUMBER` VARCHAR(10), " +
                    "`SECTION` VARCHAR(5), `LECTURE_TYPE` VARCHAR(25), `COURSE_TITLE` VARCHAR(124), `HOURS` INT, `DAYS` VARCHAR(15), `TIME` VARCHAR(50), `ROOM` VARCHAR(10)," +
                    " `INSTRUCTOR` VARCHAR(40), `MAX_ENROLLMENT` INTEGER, `SEATS_AVAILABLE` INTEGER, `MESSAGES` VARCHAR(1024), `TERM` VARCHAR(26), " +
                    "`BEGIN_DATE` VARCHAR(12), `END_DATE` VARCHAR(12), `URL` VARCHAR(1024), PRIMARY KEY(`CRN`) )");
            pstmtCsec.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void loadDB() {
        try {
            Document doc = Jsoup.connect("https://aps2.missouriwestern.edu/schedule/Default.asp?tck=201910").get();
            //this is on the landing page, the depts are in a tag called 'select'
            Elements selects = doc.select("select");
            selects.remove(0);//removes the first thing, not usable data. presumably.
            for (int i = 0; i < selects.size(); i++) {
                Elements options = selects.get(i).select("option");
                for (int j = 1; j < options.size(); j++) {
                    if (i == 0)//first selects option is for subjects
                        insert("Subjects", "SUBJECT_CODE", "SUBJECT_NAME",
                                options.get(j).val(), options.get(j).text());
                    if (i == 1)//second selects option is for departments
                        insert("Departments", "DEPARTMENT_CODE", "DEPARTMENT_NAME",
                                options.get(j).val(), options.get(j).text());
                    System.out.println(options.get(j));
                }
                System.out.println("-----------------");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void postSections() {
        try {
            int hours;
            int maximumEnrollment = 0;
            int seatsAvailable = 0;
            int sectionNumber;
            String url;
            String courseID;
            String department;
            String crn;
            String discipline;
            String lectureType;
            String courseNumber;
            String endDate = "";
            String beginDate = "";
            String term = "";
            String room = "";
            String message = "";
            String days = "";
            String time = "";
            String instructor;
            String title;
            ArrayList<Section> sections = new ArrayList<>();

            Connection conn = this.connect();
            String statement = "SELECT * FROM Departments";
            ResultSet rs = conn.createStatement().executeQuery(statement);
            while (rs.next()) {
                String dCode = rs.getString("DEPARTMENT_CODE");
                Document doc = Jsoup.connect("https://aps2.missouriwestern.edu/schedule/Default.asp?tck=201910")
                        .data("course_number", "")
                        .data("subject", "ALL")
                        .data("department", dCode)
                        .data("display_closed", "YES")
                        .data("course_type", "ALL")
                        .timeout(100 * 1000).post();

                Elements listrows = doc.select("table.results tr.list_row");
                if (listrows.size() > 0) {

                    for (Element row : listrows) {
                        Elements tds = row.select("td");
                        Elements urls = listrows.select("td a[href]");

                        int tdsCount = tds.size();

                        switch (tdsCount) {
                            case 10:
                                department = dCode;
                                crn = tds.get(0).text().trim();
                                url = urls.get(1).attr("href");
                                String s = tds.get(1).text().trim(); //getting courseID, discipline, and courseNumber from s
                                courseID = s;
                                discipline = s.substring(0, 3);//tr
                                courseNumber = s.substring(3, 6);
                                sectionNumber = Integer.parseInt(tds.get(2).text().trim());
                                lectureType = tds.get(3).text().trim();
                                title = tds.get(4).text().trim();
                                hours = Integer.parseInt(tds.get(5).text().trim());
                                days = tds.get(6).text().trim();
                                time = tds.get(7).text().trim();
                                room = tds.get(8).text().trim();
                                instructor = tds.get(9).text().trim();

                                Section sectionObject = new Section(crn, courseID, department, discipline, courseNumber, sectionNumber,
                                        lectureType, title, hours, days, time, room, instructor, maximumEnrollment, seatsAvailable, message,
                                        term, beginDate, endDate, url);
                                sections.add(sectionObject);
                                break;
                            case 5:
                                days += " | " + tds.get(1).text().trim();
                                time += " | " + tds.get(2).text().trim();
                                room += " | " + tds.get(3).text().trim();
                                break;
                        }


                    }
                    Elements detailrows = doc.select("table.results tr.detail_row");
                    for (int i = 0; i < detailrows.size(); i++) {
                        Elements seats = detailrows.get(i).select("span.course_seats");
                        parseSeatText(sections.get(i), seats.get(0));

                        Elements messages = detailrows.get(i).select("span.course_messages");
                        parseMessageText(sections.get(i), messages.get(0));

                        Elements terms = detailrows.get(i).select("span.course_term");
                        parseTermText(sections.get(i), terms.get(0));

                        Elements beginDates = detailrows.get(i).select("span.course_begins");
                        parseBeginDateText(sections.get(i), beginDates.get(0));

                        Elements endDates = detailrows.get(i).select("span.course_ends");
                        parseEndDateText(sections.get(i), endDates.get(0));
                    }
                }
            }//end of while(rs.next())
            for (int i = 0; i < sections.size(); i++) {
                insertSection(sections.get(i).crn,
                        sections.get(i).courseID,
                        sections.get(i).department,
                        sections.get(i).discipline,
                        sections.get(i).courseNumber,
                        sections.get(i).sectionNumber,
                        sections.get(i).lectureType,
                        sections.get(i).title,
                        sections.get(i).hours,
                        sections.get(i).days,
                        sections.get(i).time,
                        sections.get(i).room,
                        sections.get(i).instructor,
                        sections.get(i).maximumEnrollment,
                        sections.get(i).seatsAvailable,
                        sections.get(i).message,
                        sections.get(i).term,
                        sections.get(i).beginDate,
                        sections.get(i).endDate,
                        sections.get(i).url);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void parseSeatText(Section sec, Element seat) {
        String seatText = seat.text();
        String[] seatSplit = seatText.split("Section");
        int maxEnroll = Integer.parseInt(seatSplit[0].replaceAll("\\D", ""));
        int seatsAvail = Integer.parseInt(seatSplit[1].replaceAll("\\D", ""));

        sec.setSeatsAvailable(seatsAvail);
        sec.setMaximumEnrollment(maxEnroll - seatsAvail);
    }

    public void parseBeginDateText(Section sec, Element date) {
        String dateText = date.text();
        String beginDate = dateText.substring("Course Begins: ".length()).trim();
        sec.setBeginDate(beginDate);
    }

    public void parseEndDateText(Section sec, Element date) {
        String dateText = date.text();
        String endDate = dateText.substring("Course Ends: ".length()).trim();
        sec.setEndDate(endDate);
    }

    public void parseMessageText(Section sec, Element message) {
        String messageText = message.text();
        sec.setMessage(messageText);
    }

    public void parseTermText(Section sec, Element term) {
        String termText = term.text();
        sec.setTerm(termText);
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.dropTables();
        app.createTables();
        app.loadDB();
        app.postSections();
    }

    public class Section {
        int hours;
        int maximumEnrollment;
        int seatsAvailable;
        int sectionNumber;
        int people;
        String url;
        String courseID;
        String department;
        String crn;
        String discipline;
        String lectureType;
        String courseNumber;
        String endDate;
        String beginDate;
        String term;
        String room;
        String message;
        String days;
        String time;
        String instructor;
        String title;

        public Section() {
            hours = 0;
            maximumEnrollment = 0;
            seatsAvailable = 0;
            sectionNumber = 0;
            people = 0;//people is new
            url = "";
            courseID = "";
            crn = "";
            discipline = "";
            lectureType = "";
            courseNumber = "";
            endDate = "";
            beginDate = "";
            term = "";
            room = "";
            message = "";
            days = "";
            time = "";
            instructor = "";
            title = "";
        }

        public Section(String crn, String courseID, String department, String discipline, String courseNumber, int sectionNumber,
                       String lectureType, String title, int hours, String days, String time, String room, String instructor,
                       int maximumEnrollment, int seatsAvailable, String message,
                       String term, String beginDate, String endDate, String url) {
            this.hours = hours;
            this.maximumEnrollment = maximumEnrollment;
            this.seatsAvailable = seatsAvailable;
            this.sectionNumber = sectionNumber;
            this.url = url;
            this.courseID = courseID;
            this.department = department;
            this.crn = crn;
            this.discipline = discipline;
            this.lectureType = lectureType;
            this.courseNumber = courseNumber;
            this.endDate = endDate;
            this.beginDate = beginDate;
            this.term = term;
            this.room = room;
            this.message = message;
            this.days = days;
            this.time = time;
            this.instructor = instructor;
            this.title = title;
        }

        @Override
        public String toString() {
            return "Section{" +
                    "hours=" + hours +
                    ", maximumEnrollment=" + maximumEnrollment +
                    ", seatsAvailable=" + seatsAvailable +
                    ", sectionNumber=" + sectionNumber +
                    ", url='" + url + '\'' +
                    ", courseID='" + courseID + '\'' +
                    ", department='" + department + '\'' +
                    ", crn='" + crn + '\'' +
                    ", discipline='" + discipline + '\'' +
                    ", lectureType='" + lectureType + '\'' +
                    ", courseNumber='" + courseNumber + '\'' +
                    ", endDate='" + endDate + '\'' +
                    ", beginDate='" + beginDate + '\'' +
                    ", term='" + term + '\'' +
                    ", room='" + room + '\'' +
                    ", message='" + message + '\'' +
                    ", days='" + days + '\'' +
                    ", time='" + time + '\'' +
                    ", instructor='" + instructor + '\'' +
                    ", title='" + title + '\'' +
                    '}';
        }

        public int getHours() {
            return hours;
        }

        public void setHours(int hours) {
            this.hours = hours;
        }

        public int getMaximumEnrollment() {
            return maximumEnrollment;
        }

        public void setMaximumEnrollment(int maximumEnrollment) {
            this.maximumEnrollment = maximumEnrollment;
        }

        public int getSeatsAvailable() {
            return seatsAvailable;
        }

        public void setSeatsAvailable(int seatsAvailable) {
            this.seatsAvailable = seatsAvailable;
        }

        public int getSectionNumber() {
            return sectionNumber;
        }

        public void setSectionNumber(int sectionNumber) {
            this.sectionNumber = sectionNumber;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCourseID() {
            return courseID;
        }

        public void setCourseID(String courseID) {
            this.courseID = courseID;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public int getPeople() {
            return people;
        }

        public void setPeople(int people) {
            this.people = people;
        }

        public String getCrn() {
            return crn;
        }

        public void setCrn(String crn) {
            this.crn = crn;
        }

        public String getDiscipline() {
            return discipline;
        }

        public void setDiscipline(String discipline) {
            this.discipline = discipline;
        }

        public String getLectureType() {
            return lectureType;
        }

        public void setLectureType(String lectureType) {
            this.lectureType = lectureType;
        }

        public String getCourseNumber() {
            return courseNumber;
        }

        public void setCourseNumber(String courseNumber) {
            this.courseNumber = courseNumber;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public String getBeginDate() {
            return beginDate;
        }

        public void setBeginDate(String beginDate) {
            this.beginDate = beginDate;
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public String getRoom() {
            return room;
        }

        public void setRoom(String room) {
            this.room = room;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getDays() {
            return days;
        }

        public void setDays(String days) {
            this.days = days;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getInstructor() {
            return instructor;
        }

        public void setInstructor(String instructor) {
            this.instructor = instructor;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
