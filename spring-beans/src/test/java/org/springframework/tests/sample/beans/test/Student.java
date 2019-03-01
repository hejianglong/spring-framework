package org.springframework.tests.sample.beans.test;

public class Student {

	private StudentService studentService;

	public StudentService getStudentService() {
		return studentService;
	}

	public void setStudentService(StudentService studentService) {
		this.studentService = studentService;
	}

	@Override
	public String toString() {
		return "Student{" +
				"studentService=" + studentService +
				'}';
	}
}
