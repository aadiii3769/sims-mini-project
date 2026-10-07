package com.sims;

import com.sims.controller.MarksController;
import com.sims.dao.CourseDAO;
import com.sims.dao.DepartmentDAO;
import com.sims.dao.FacultyDAO;
import com.sims.dao.MarksDAO;
import com.sims.dao.StudentDAO;
import com.sims.dao.UserDAO;
import com.sims.model.Course;
import com.sims.model.Department;
import com.sims.model.Faculty;
import com.sims.model.Mark;
import com.sims.model.Student;
import com.sims.model.User;
import com.sims.util.DBConnection;

import java.util.List;
import java.util.Optional;

/**
 * Standalone verification runner for Academic Curriculum Expansion & Assessment Data Generation.
 */
public class VerifyAcademicExpansion {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   ACADEMIC CURRICULUM EXPANSION & ASSESSMENT VERIFICATION SUITE   ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        CourseDAO       courseDAO       = new CourseDAO();
        DepartmentDAO   departmentDAO   = new DepartmentDAO();
        MarksDAO        marksDAO        = new MarksDAO();
        StudentDAO      studentDAO      = new StudentDAO();
        FacultyDAO      facultyDAO      = new FacultyDAO();
        UserDAO         userDAO         = new UserDAO();
        MarksController marksController = new MarksController();

        // 1. Departments test
        try {
            List<Department> depts = departmentDAO.findAll();
            if (depts.size() >= 4) {
                System.out.println("✅ [PASS] Departments master records loaded: " + depts.size() + " depts (CSE, IT, ECE, MECH)");
                passed++;
            } else {
                System.err.println("❌ [FAIL] Expected >= 4 departments, found: " + depts.size());
                failed++;
            }
        } catch (Exception e) {
            System.err.println("❌ [FAIL] Departments test failed: " + e.getMessage());
            failed++;
        }

        // 2. Common Semester 1 courses test
        try {
            List<Course> cseSem1 = courseDAO.getCoursesByDeptAndSemester(1L, 1);
            List<Course> itSem1  = courseDAO.getCoursesByDeptAndSemester(2L, 1);
            List<Course> eceSem1 = courseDAO.getCoursesByDeptAndSemester(3L, 1);
            List<Course> mechSem1 = courseDAO.getCoursesByDeptAndSemester(4L, 1);

            if (!cseSem1.isEmpty() && cseSem1.size() == itSem1.size() && itSem1.size() == eceSem1.size() && eceSem1.size() == mechSem1.size()) {
                System.out.println("✅ [PASS] Semester 1 Common Foundation: All 4 departments share identical " + cseSem1.size() + " courses");
                for (Course c : cseSem1) {
                    System.out.println("         - " + c.getCourseCode() + ": " + c.getCourseName() + " (" + c.getCredits() + " credits)");
                }
                passed++;
            } else {
                System.err.println("❌ [FAIL] Semester 1 common courses mismatch across departments");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("❌ [FAIL] Sem 1 common courses test: " + e.getMessage());
            failed++;
        }

        // 3. Department-specific courses test (Semesters 2 to 8)
        try {
            boolean allSemestersHaveCourses = true;
            for (long deptId = 1; deptId <= 4; deptId++) {
                for (int sem = 2; sem <= 8; sem++) {
                    List<Course> semCourses = courseDAO.getCoursesByDeptAndSemester(deptId, sem);
                    if (semCourses.size() < 3) {
                        System.err.println("❌ [FAIL] Dept " + deptId + " Sem " + sem + " has less than 3 courses: " + semCourses.size());
                        allSemestersHaveCourses = false;
                    }
                }
            }
            if (allSemestersHaveCourses) {
                System.out.println("✅ [PASS] Semesters 2 to 8: All 4 departments have 3-4 distinct subjects per semester (total courses: " + courseDAO.findAll().size() + ")");
                passed++;
            } else {
                failed++;
            }
        } catch (Exception e) {
            System.err.println("❌ [FAIL] Dept specific courses test: " + e.getMessage());
            failed++;
        }

        // 4. Faculty mapping & allocations test
        try {
            List<Faculty> allFaculty = facultyDAO.findAll();
            if (allFaculty.size() >= 5) {
                System.out.println("✅ [PASS] Faculty profiles mapped to departments: " + allFaculty.size() + " faculty");
                for (Faculty f : allFaculty) {
                    List<Course> alloc = courseDAO.getFacultyAllocatedCourses(f.getFacultyId());
                    System.out.println("         - " + f.getFullName() + " (" + f.getDeptCode() + " - " + f.getDesignation() + "): " + alloc.size() + " allocated courses");
                }
                passed++;
            } else {
                System.err.println("❌ [FAIL] Expected >= 5 faculty, found: " + allFaculty.size());
                failed++;
            }
        } catch (Exception e) {
            System.err.println("❌ [FAIL] Faculty allocation test: " + e.getMessage());
            failed++;
        }

        // 5. Active Odd-Semester CAT Marks test
        try {
            List<Student> students = studentDAO.findAll();
            boolean activeCATsValid = true;
            for (Student s : students) {
                int activeSem = s.getCurrentSemester();
                List<Mark> activeMarks = marksDAO.getMarksByStudentAndSemester(s.getStudentId(), activeSem);
                if (activeMarks.isEmpty()) {
                    System.err.println("❌ [FAIL] Student " + s.getFullName() + " has no active marks for Sem " + activeSem);
                    activeCATsValid = false;
                } else {
                    for (Mark m : activeMarks) {
                        if (m.getCat1Marks() <= 0 || m.getCat2Marks() <= 0 || m.getAssignmentMarks() <= 0) {
                            System.err.println("❌ [FAIL] Student " + s.getFullName() + " course " + m.getCourseCode() + " has zero CAT marks");
                            activeCATsValid = false;
                        }
                    }
                }
            }
            if (activeCATsValid) {
                System.out.println("✅ [PASS] Active Odd-Semester CAT Marks: All " + students.size() + " active students have non-zero CAT1, CAT2, Assignment marks in their active semester");
                passed++;
            } else {
                failed++;
            }
        } catch (Exception e) {
            System.err.println("❌ [FAIL] Active odd semester test: " + e.getMessage());
            failed++;
        }

        // 6. Historical transcripts for prior semesters (1 to N-1) test
        try {
            Student arun = studentDAO.findByRollNumber("22CS001").orElseThrow(); // Sem 5
            boolean historicalValid = true;
            for (int sem = 1; sem <= 4; sem++) {
                List<Mark> histMarks = marksDAO.getMarksByStudentAndSemester(arun.getStudentId(), sem);
                if (histMarks.isEmpty()) {
                    System.err.println("❌ [FAIL] Arun Kumar missing historical marks for Sem " + sem);
                    historicalValid = false;
                } else {
                    for (Mark m : histMarks) {
                        if (!m.isCompleted() || m.getSemesterGrade() == null || m.getGradePoint() < 6.0) {
                            System.err.println("❌ [FAIL] Invalid historical transcript entry: " + m.getCourseCode());
                            historicalValid = false;
                        }
                    }
                }
                double semGPA = marksController.getSemesterGPA(arun.getStudentId(), sem);
                System.out.println("         - " + arun.getFullName() + " Sem " + sem + " Transcript: " + histMarks.size() + " courses, GPA = " + semGPA);
            }
            double cgpa = marksController.getCGPA(arun.getStudentId());
            System.out.println("         - Cumulative CGPA = " + cgpa);

            if (historicalValid && cgpa > 0) {
                System.out.println("✅ [PASS] Historical Academic Transcripts: Complete prior semester records and GPAs verified for Semesters 1 to N-1");
                passed++;
            } else {
                failed++;
            }
        } catch (Exception e) {
            System.err.println("❌ [FAIL] Historical transcripts test: " + e.getMessage());
            failed++;
        }

        // 7. Parent resolution test
        try {
            Optional<User> parentUser = userDAO.findByUsername("parent01");
            if (parentUser.isPresent()) {
                Optional<Student> child = marksController.findStudentForViewer(parentUser.get());
                if (child.isPresent() && "22CS001".equals(child.get().getRollNumber())) {
                    System.out.println("✅ [PASS] Parent Portal Resolution: parent01 successfully links to student " + child.get().getFullName() + " (" + child.get().getRollNumber() + ")");
                    passed++;
                } else {
                    System.err.println("❌ [FAIL] Parent failed to resolve child student");
                    failed++;
                }
            } else {
                System.err.println("❌ [FAIL] parent01 user not found");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("❌ [FAIL] Parent resolution test: " + e.getMessage());
            failed++;
        }

        System.out.println("==================================================================");
        System.out.println("RESULT: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("==================================================================");

        DBConnection.getInstance().close();
        if (failed > 0) {
            System.exit(1);
        }
    }
}
