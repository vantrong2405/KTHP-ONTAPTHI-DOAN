
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Date;
import javax.swing.event.ChangeEvent;

public class TaskClient extends JFrame {

	private JTextField txtTaskName, txtDescription, txtSearch;
	private JSpinner txtDueDate;
	private JComboBox<String> comboUser, comboCategory, comboStatus;
	private JTable taskTable;
	private DefaultTableModel tableModel;
	private TaskService taskService;
	int taskId = -1;
	String name = "";
	String description = "";
	Date dueDate = null;
	String userFullName = "";
	String categoryFullname = "";
	String status = "";
	int userId = -1;
	int categoryId = -1;

	private static final String DB_URL = "jdbc:mysql://localhost:3306/test_db";
	private static final String DB_USER = "root";
	private static final String DB_PASSWORD = "Admin123@";

	public TaskClient() {
		setTitle("Task Management");
		setSize(900, 500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		try {
			taskService = (TaskService) Naming.lookup("rmi://localhost/TaskService");
			System.out.println("RMI Server connected successfully.");
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Không thể kết nối với Server RMI.", "Lỗi", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}

		initUI();
		loadAllTasks();
	}

	private void initUI() {
		setTitle("Ứng dụng Quản Lý Công Việc");
		setSize(1000, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel(new BorderLayout());
		JPanel inputPanel = new JPanel(new GridLayout(8, 2, 10, 10)); 

		inputPanel.add(new JLabel("Tên công việc"));
		txtTaskName = new JTextField();
		inputPanel.add(txtTaskName);

		inputPanel.add(new JLabel("Mô tả"));
		txtDescription = new JTextField();
		inputPanel.add(txtDescription);

		inputPanel.add(new JLabel("Ngày hết hạn"));
		SpinnerDateModel model = new SpinnerDateModel();
		txtDueDate = new JSpinner(model);

		JSpinner.DateEditor editor = new JSpinner.DateEditor(txtDueDate, "yyyy-MM-dd");
		txtDueDate.setEditor(editor);

		inputPanel.add(txtDueDate);

		JFrame frame = new JFrame("Chọn ngày");

		inputPanel.add(new JLabel("Tên người dùng"));
		comboUser = new JComboBox<>();
		loadUsers();
		inputPanel.add(comboUser);

		inputPanel.add(new JLabel("Danh mục"));
		comboCategory = new JComboBox<>();
		loadCategories();
		inputPanel.add(comboCategory);

		inputPanel.add(new JLabel("Trạng thái"));
		comboStatus = new JComboBox<>(new String[] { "Pending", "In Progress", "Completed" });
		inputPanel.add(comboStatus);

		inputPanel.add(new JLabel("Tìm kiếm"));
		txtSearch = new JTextField();
		inputPanel.add(txtSearch);

		// Buttons
		JPanel buttonPanel = new JPanel();
		JButton btnAdd = new JButton("Thêm công việc");
		JButton btnUpdate = new JButton("Cập nhật công việc");
		JButton btnDelete = new JButton("Xóa công việc");
		JButton btnSearch = new JButton("Tìm kiếm");
		JButton btnLoadData = new JButton("Tải lại danh sách");
		JButton btnClearDataInput = new JButton("Làm mới nhập liệu");

		buttonPanel.add(btnAdd);
		buttonPanel.add(btnUpdate);
		buttonPanel.add(btnDelete);
		buttonPanel.add(btnSearch);
		buttonPanel.add(btnLoadData);
		buttonPanel.add(btnClearDataInput);
		// Table for displaying tasks
		tableModel = new DefaultTableModel();
		taskTable = new JTable(tableModel);
		tableModel.addColumn("ID");
		tableModel.addColumn("Tên công việc");
		tableModel.addColumn("Mô tả");
		tableModel.addColumn("Ngày hết hạn");
		tableModel.addColumn("Tên người dùng");
		tableModel.addColumn("Danh mục");
		tableModel.addColumn("Trạng thái");

		JScrollPane scrollPane = new JScrollPane(taskTable);
		panel.add(inputPanel, BorderLayout.NORTH);
		panel.add(scrollPane, BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		taskTable.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					onTableCellEdited();
				}
			}
		});

		setContentPane(panel);
		setTableCellEditor();
		// Add listeners
		btnClearDataInput.addActionListener(e -> resetValueInput());
		btnAdd.addActionListener(e -> addTask());
		btnUpdate.addActionListener(e -> updateTask());
		btnDelete.addActionListener(e -> deleteTask());
		btnSearch.addActionListener(e -> searchTasks());
		btnLoadData.addActionListener(e -> loadAllTasks());
	}

	private void resetValueInput() {
		Date currentDate = new Date();
		txtDescription.setText("");
		txtSearch.setText("");
		txtTaskName.setText("");
		txtDueDate.setValue(currentDate);
		comboCategory.setSelectedIndex(0);
		comboStatus.setSelectedIndex(0);
		comboUser.setSelectedIndex(0);
	}

	private JComboBox<String> createUserComboBox() {
		JComboBox<String> userComboBox = new JComboBox<>();
		try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
			String sql = "SELECT full_name FROM users";
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				userComboBox.addItem(rs.getString("full_name"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Không thể tải danh sách người dùng.", "Lỗi",
					JOptionPane.ERROR_MESSAGE);
		}
		return userComboBox;
	}

	private JComboBox<String> createCategoryComboBox() {
		JComboBox<String> categoryComboBox = new JComboBox<>();
		try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
			String sql = "SELECT name FROM categories";
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				categoryComboBox.addItem(rs.getString("name"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Không thể tải danh sách danh mục.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
		return categoryComboBox;
	}

	private JComboBox<String> createStatusComboBox() {
		return new JComboBox<>(new String[] { "Pending", "In Progress", "Completed" });
	}

	private void setTableCellEditor() {
		JComboBox<String> userComboBox = createUserComboBox();
		TableColumn userColumn = taskTable.getColumnModel().getColumn(4);
		userColumn.setCellEditor(new DefaultCellEditor(userComboBox));

		JComboBox<String> categoryComboBox = createCategoryComboBox();
		TableColumn categoryColumn = taskTable.getColumnModel().getColumn(5);
		categoryColumn.setCellEditor(new DefaultCellEditor(categoryComboBox));

		JComboBox<String> statusComboBox = createStatusComboBox();
		TableColumn statusColumn = taskTable.getColumnModel().getColumn(6);
		statusColumn.setCellEditor(new DefaultCellEditor(statusComboBox));
	}

	private void loadAllTasks() {
		try {
			List<Task> tasks = taskService.getAllTasks();
			if (tasks == null || tasks.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Không có công việc nào để tải.", "Thông báo",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			int selectedRow = taskTable.getSelectedRow();
			tableModel.setRowCount(0);

			for (Task task : tasks) {
				tableModel.addRow(new Object[] { task.getId(), task.getName(), task.getDescription(), task.getDueDate(),
						task.getUserFullName(), task.getCategoryName(), task.getStatus() });
			}

			System.out.println("Load thành công");
			if (selectedRow >= 0 && selectedRow < taskTable.getRowCount()) {
				taskTable.setRowSelectionInterval(selectedRow, selectedRow);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Không thể tải danh sách công việc.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		} finally {
			tableModel.addTableModelListener(e -> onTableCellEdited());
		}
	}

	private void loadUsers() {
		try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
			String sql = "SELECT id, full_name FROM users";
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				comboUser.addItem(rs.getString("full_name"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Không thể tải danh sách người dùng.", "Lỗi",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void loadCategories() {
		try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
			String sql = "SELECT id, name FROM categories";
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				comboCategory.addItem(rs.getString("name"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Không thể tải danh sách danh mục.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void addTask() {
		String name = txtTaskName.getText();
		String description = txtDescription.getText();
		Date dueDateValue = (Date) txtDueDate.getValue();
		Date currentDate = new Date();
		currentDate = Utils.resetTime(currentDate);
		dueDateValue = Utils.resetTime(dueDateValue);

		if (dueDateValue.before(currentDate)) {
			JOptionPane.showMessageDialog(this, "Ngày hết hạn không thể nhỏ hơn ngày hiện tại.", "Lỗi",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		String utilDate = txtDueDate.getValue().toString();
		String dueDateString = Utils.convertToDateFormat(utilDate);

		String userFullName = (String) comboUser.getSelectedItem();
		String category = (String) comboCategory.getSelectedItem();
		String status = (String) comboStatus.getSelectedItem();
		if (name.isEmpty() || description.isEmpty() || dueDateString.isEmpty() || userFullName.isEmpty()
				|| category.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin.", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			int userId = Utils.getUserId(userFullName);
			int categoryId = Utils.getCategoryId(category);
			taskService.addTask(name, description, dueDateString, userId, categoryId, status);
			JOptionPane.showMessageDialog(this, "Công việc đã được thêm.", "Thông báo",
					JOptionPane.INFORMATION_MESSAGE);
			loadAllTasks();
		} catch (RemoteException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Không thể thêm công việc.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void onTableCellEdited() {
		if (taskTable.getRowCount() == 0) {
			return;
		}

		int row = taskTable.getSelectedRow();
		int column = taskTable.getSelectedColumn();

		if (row < 0 || row >= taskTable.getRowCount() || column < 0 || column >= taskTable.getColumnCount()) {
			return;
		}

		taskId = (int) taskTable.getValueAt(row, 0);
		name = (String) taskTable.getValueAt(row, 1);
		description = (String) taskTable.getValueAt(row, 2);
		dueDate = parseDate(taskTable.getValueAt(row, 3));
		if (dueDate == null) {
			JOptionPane.showMessageDialog(this, "Dữ liệu ngày không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
			loadAllTasks();
			return;
		}

		userFullName = (String) taskTable.getValueAt(row, 4);
		categoryFullname = (String) taskTable.getValueAt(row, 5);
		status = (String) taskTable.getValueAt(row, 6);
		userId = Utils.getUserId(userFullName);
		categoryId = Utils.getCategoryId(categoryFullname);
	}

	private Date parseDate(Object value) {
		try {
			if (value instanceof String) {
				String dateString = (String) value;
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				sdf.setLenient(false); 
				Date date = sdf.parse(dateString);
				if (!dateString.equals(sdf.format(date))) {
					return null;
				}

				return date;
			} else if (value instanceof Date) {
				return (Date) value; 
			}
		} catch (Exception e) {
			e.printStackTrace(); 
		}
		return null; 
	}

	private void updateTask() {
		int selectedRow = taskTable.getSelectedRow();

		if (selectedRow == -1) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn công việc cần cập nhật.", "Lỗi",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		String name = (String) taskTable.getValueAt(selectedRow, 1);
		String description = (String) taskTable.getValueAt(selectedRow, 2);
		dueDate = parseDate(taskTable.getValueAt(selectedRow, 3));

		if (dueDate == null) {
			JOptionPane.showMessageDialog(this, "Dữ liệu ngày không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
			loadAllTasks();
			return;
		}

		String userFullName = (String) taskTable.getValueAt(selectedRow, 4);
		String categoryFullname = (String) taskTable.getValueAt(selectedRow, 5);
		String status = (String) taskTable.getValueAt(selectedRow, 6);

		Date currentDate = Utils.resetTime(new Date());
		dueDate = Utils.resetTime(dueDate);
		if (dueDate.before(currentDate)) {
			JOptionPane.showMessageDialog(this, "Ngày hết hạn không thể nhỏ hơn ngày hiện tại.", "Lỗi",
					JOptionPane.ERROR_MESSAGE);
			loadAllTasks();
			return;
		}

		if (name.isEmpty() || description.isEmpty() || dueDate == null || userFullName == null
				|| categoryFullname == null || status == null) {
			JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin.", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		int userId = Utils.getUserId(userFullName);
		int categoryId = Utils.getCategoryId(categoryFullname);

		if (userId == -1 || categoryId == -1) {
			JOptionPane.showMessageDialog(this, "Không thể tìm thấy người dùng hoặc danh mục.", "Lỗi",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			taskService.updateTask(taskId, name, description, Utils.convertToDateFormat(dueDate.toString()), userId,
					categoryId, status);
			JOptionPane.showMessageDialog(this, "Công việc đã được cập nhật.", "Thông báo",
					JOptionPane.INFORMATION_MESSAGE);
			loadAllTasks();
		} catch (RemoteException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "Không thể cập nhật công việc.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void deleteTask() {
		int selectedRow = taskTable.getSelectedRow();
		if (selectedRow == -1) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn công việc để xóa.", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		int taskId = (int) taskTable.getValueAt(selectedRow, 0);
		try {
			taskService.deleteTask(taskId);
			JOptionPane.showMessageDialog(this, "Công việc đã được xóa.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
			loadAllTasks();
		} catch (RemoteException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Không thể xóa công việc.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void searchTasks() {
		String searchQuery = txtSearch.getText();
		if (searchQuery.isEmpty()) {
			loadAllTasks();
			return;
		}

		try {
			List<Task> tasks = taskService.searchTasks(searchQuery);
			tableModel.setRowCount(0);
			for (Task task : tasks) {
				tableModel.addRow(new Object[] { task.getId(), task.getName(), task.getDescription(), task.getDueDate(),
						task.getUserFullName(), task.getCategoryName(), task.getStatus() });
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Không thể tìm kiếm công việc.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new TaskClient().setVisible(true);
			}
		});
	}
}
