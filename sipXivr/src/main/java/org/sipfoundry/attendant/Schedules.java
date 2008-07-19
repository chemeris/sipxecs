/*
 *
 *
 * Copyright (C) 2008 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 */
package org.sipfoundry.attendant;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class Schedules {
    static final Logger LOG = Logger.getLogger("org.sipfoundry.sipxivr");

    class TimeRange {
        private Date m_from; // Starts at this time (inclusive) (Only time part is used, date is ignored)
        private Date m_to; // Ends one second before this time
        public Date getFrom() {
            return m_from;
        }
        public void setFrom(Date from) {
            m_from = from;
        }
        public Date getTo() {
            return m_to;
        }
        public void setTo(Date to) {
            m_to = to;
        }
    }

    class Day {
        private int m_dayOfWeek; // As defined in Calendar
        private TimeRange m_range; // Time during that day
        public int getDayOfWeek() {
            return m_dayOfWeek;
        }
        public void setDayOfWeek(int dayOfWeek) {
            m_dayOfWeek = dayOfWeek;
        }
        public TimeRange getRange() {
            return m_range;
        }
        public void setRange(TimeRange range) {
            m_range = range;
        }
    }

    class Holidays {
        private ArrayList<Date> m_dates = new ArrayList<Date>(); // Dates that are holidays
        private String m_attendantName; // Attendant name to run
        
        public void add(Date date) {
            m_dates.add(date);
        }
        public ArrayList<Date> getDates() {
            return m_dates;
        }
        public void setDates(ArrayList<Date> dates) {
            m_dates = dates;
        }
        public String getAttendantName() {
            return m_attendantName;
        }
        public void setAttendantName(String attendantName) {
            m_attendantName = attendantName;
        }
    }

    class Hours {
        private ArrayList<Day> m_days = new ArrayList<Day>(); // The regular hours
        private String m_regularHoursAttendantName; // Attendant name to run during regular hours
        private String m_afterHoursAttendantName; // Attendant name to run not during regular hours
        public void add(Day day) {
            m_days.add(day);
        }
        public ArrayList<Day> getDays() {
            return m_days;
        }
        public void setDays(ArrayList<Day> days) {
            m_days = days;
        }
        public String getRegularHoursAttendantName() {
            return m_regularHoursAttendantName;
        }
        public void setRegularHoursAttendantName(String regularHoursAttendantName) {
            m_regularHoursAttendantName = regularHoursAttendantName;
        }
        public String getAfterHoursAttendantName() {
            return m_afterHoursAttendantName;
        }
        public void setAfterHoursAttendantName(String afterHoursAttendantName) {
            m_afterHoursAttendantName = afterHoursAttendantName;
        }
    }

    private Holidays m_holidays;
    private Hours m_hours;
    private boolean m_specialOperation;
    private String m_specialAutoAttendant;

    Schedules() {
        m_holidays = new Holidays();
        m_hours = new Hours();
    }

    /**
     * Load the schedule for a particular day
     * 
     * @param dayOfWeek
     * @param node
     * @throws ParseException
     */
    private void loadDay(int dayOfWeek, Node node) throws ParseException {
        DateFormat timeFormat = new SimpleDateFormat("HH:mm");

        Day day = new Day();
        day.m_range = new TimeRange();
        day.m_dayOfWeek = dayOfWeek;

        Node fromTo = node.getFirstChild();
        while (fromTo != null) {
            if (fromTo.getNodeType() == Node.ELEMENT_NODE) {
                String name = fromTo.getNodeName();
                if (name.contentEquals("from")) {
                    String fromString = fromTo.getTextContent().trim();
                    day.m_range.m_from = timeFormat.parse(fromString);
                } else if (name.contentEquals("to")) {
                    String toString = fromTo.getTextContent().trim();
                    day.m_range.m_to = timeFormat.parse(toString);
                }
            }
            fromTo = fromTo.getNextSibling();
        }
        m_hours.m_days.add(day);
    }

    /**
     * Load the schedules from an XML node
     * 
     * @param schedules
     */
    void loadSchedules(Node schedules) {
        String parm = "unknown";
        DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
        try {
            for (Node schedule = schedules.getFirstChild(); schedule != null; schedule = schedule
                    .getNextSibling()) {

                if (schedule.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                // First find the Holidays
                if (schedule.getNodeName().equals(parm = "holiday")) {
                    for (Node next = schedule.getFirstChild(); next != null; next = next
                            .getNextSibling()) {
                        if (next.getNodeType() == Node.ELEMENT_NODE) {
                            String name = next.getNodeName();
                            if (name.contentEquals("name")) {
                                m_holidays.m_attendantName = next.getTextContent().trim();
                            } else if (name.contentEquals("date")) {
                                String dateString = next.getTextContent().trim();
                                Date date = dateFormat.parse(dateString);
                                m_holidays.m_dates.add(date);
                            }
                        }
                    }
                    continue;
                }

                // Then the regular hours
                if (schedule.getNodeName().equals(parm = "regularhours")) {
                    for (Node next = schedule.getFirstChild(); next != null; next = next
                            .getNextSibling()) {
                        if (next.getNodeType() == Node.ELEMENT_NODE) {
                            String name = next.getNodeName();
                            if (name.contentEquals("name")) {
                                m_hours.m_regularHoursAttendantName = next.getTextContent()
                                        .trim();
                            } else if (name.contentEquals("monday")) {
                                loadDay(Calendar.MONDAY, next);
                            } else if (name.contentEquals("tuesday")) {
                                loadDay(Calendar.TUESDAY, next);
                            } else if (name.contentEquals("wednesday")) {
                                loadDay(Calendar.WEDNESDAY, next);
                            } else if (name.contentEquals("thursday")) {
                                loadDay(Calendar.THURSDAY, next);
                            } else if (name.contentEquals("friday")) {
                                loadDay(Calendar.FRIDAY, next);
                            } else if (name.contentEquals("saturday")) {
                                loadDay(Calendar.SATURDAY, next);
                            } else if (name.contentEquals("sunday")) {
                                loadDay(Calendar.SUNDAY, next);
                            }
                        }
                    }
                    continue;
                }

                // Then the after hours
                if (schedule.getNodeName().equals(parm = "afterhours")) {
                    for (Node next = schedule.getFirstChild(); next != null; next = next
                            .getNextSibling()) {
                        if (next.getNodeType() == Node.ELEMENT_NODE) {
                            String name = next.getNodeName();
                            if (name.contentEquals("name")) {
                                m_hours.m_afterHoursAttendantName = next.getTextContent().trim();
                            }
                        }
                    }
                    continue;
                }
            }
        } catch (Throwable t) {
            LOG.error("Trouble with schedules section " + parm, t);
        }
    }

    /**
     * Load the "organization preferences" XML file
     * 
     * @param filename
     */
    public void loadPrefs(String filename) {
        try {
            File organizationFile = new File(filename);
            if (organizationFile.exists()) {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();
                Document schedule = builder.parse(organizationFile);
                Node next = schedule.getFirstChild();
                while (next != null) {
                    if (next.getNodeType() == Node.ELEMENT_NODE) {
                        String name = next.getNodeName();
                        if (name.contentEquals("specialoperation")) {
                            m_specialOperation = Boolean.parseBoolean(next.getTextContent().trim());
                        } else if (name.contentEquals("autoattendant")) {
                            m_specialAutoAttendant = next.getTextContent().trim();
                        }
                    }
                    next = next.getNextSibling();
                }
            } else {
                LOG.info(String.format("File %s does not exist.  Using defaults.", filename));
            }
        } catch (Throwable t) {
            LOG.error("Trouble reading prefs file " + filename, t);
        }
    }

    /**
     * Determine which Attendant name is in effect at a particular Date or time
     * 
     * @param date
     * @return
     */
    public String getAttendant(Date date) {
        // Always use the special AA if specialOperation is in effect
        if (m_specialOperation) {
            LOG.info("Special Operation AutoAttendant is in effect.");
            return m_specialAutoAttendant;
        }

        // Otherwise check the schedule to see which AA to use.
        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.setTime(date);

        // First check if date is a holiday
        for (Date from : m_holidays.m_dates) {
            // Test if date >= from
            if (!date.before(from)) {
                // Add a day to the start to get the end
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(from);
                calendar.add(Calendar.DAY_OF_WEEK, 1);
                Date to = calendar.getTime();
                // Test if nowDate < to 
                if (date.before(to)) {
                    // Yep, it is
                    DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");

                    LOG.info("Using holiday AutoAttendant as " + dateFormat.format(from)
                            + " is a holiday");
                    return m_holidays.m_attendantName;
                }
            }
        }

        DateFormat timeFormat = new SimpleDateFormat("EEE HH:mm");
        Calendar justTime = Calendar.getInstance();
        justTime.clear();
        justTime.set(Calendar.HOUR_OF_DAY, nowCalendar.get(Calendar.HOUR_OF_DAY));
        justTime.set(Calendar.MINUTE, nowCalendar.get(Calendar.MINUTE));
        Date time = justTime.getTime();
        // Next, check if date is within regular hours
        for (Day day : m_hours.m_days) {
            // Check if the day is the same
            if (day.m_dayOfWeek == nowCalendar.get(Calendar.DAY_OF_WEEK)) {
                // Then check if the hours match too.
                if (!time.before(day.m_range.m_from)) {
                    if (time.before(day.m_range.m_to)) {
                        // Yep, it does
                        LOG.info("Using regular hours AutoAttendant as "
                                + timeFormat.format(date) + " is regular hours");
                        return m_hours.m_regularHoursAttendantName;
                    }
                }

            }
        }

        // Nope.  Must be after hours
        LOG.info("Using after hours AutoAttendant as " + timeFormat.format(date)
                + " is after hours");
        return m_hours.m_afterHoursAttendantName;

    }

    public Holidays getHolidays() {
        return m_holidays;
    }

    public void setHolidays(Holidays holidays) {
        m_holidays = holidays;
    }

    public Hours getHours() {
        return m_hours;
    }

    public void setHours(Hours hours) {
        m_hours = hours;
    }

    public boolean isSpecialOperation() {
        return m_specialOperation;
    }

    public void setSpecialOperation(boolean specialOperation) {
        m_specialOperation = specialOperation;
    }

    public String getSpecialAutoAttendant() {
        return m_specialAutoAttendant;
    }

    public void setSpecialAutoAttendant(String specialAutoAttendant) {
        m_specialAutoAttendant = specialAutoAttendant;
    }
}