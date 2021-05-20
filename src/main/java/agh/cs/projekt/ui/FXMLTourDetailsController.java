package agh.cs.projekt.ui;

import agh.cs.projekt.UserHolder;
import agh.cs.projekt.models.Customer;
import agh.cs.projekt.models.Reservation;
import agh.cs.projekt.models.Tour;
import agh.cs.projekt.utils.ImageController;
import agh.cs.projekt.utils.PersistentAlert;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class FXMLTourDetailsController implements Initializable {

    @FXML
    public Label tour_name;
    @FXML
    public ImageView image_view;
    @FXML
    public Label tour_country;
    @FXML
    public Label tour_description;
    @FXML
    public Label tour_date;
    @FXML
    public Label tour_price;
    @FXML
    public Label tour_places;
    @FXML
    public Button return_button;
    @FXML
    public Label customer_loading;
    @FXML
    public Label customer_reservations;
    @FXML
    public Button button_make_reservation;
    @FXML
    public Button button_cancel_reservation;
    @FXML
    public HBox hbox_customer_reservations;
    @FXML
    public HBox hbox_reservation_controls;

    //private caches
    private Tour tour = null;
    private Customer customer = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    public void displayTour(Tour tour){
        if (tour == null){
            System.err.println("Trying to display a null tour!");
            //TODO revert to previous screen ~W
        }

        this.tour = tour;
        this.customer = UserHolder.getInstance().getUser().getCustomer();
        ImageController.loadFromSource(image_view, tour.getImage());
        tour_name.setText(tour.getName());
        tour_country.setText(tour.getCountry().toString());
        tour_description.setText(tour.getDescription());
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        tour_date.setText(sdf.format(tour.getTourDate()));
        tour_price.setText(String.format("%.2f", tour.getPrice()) + "z\u0142"); // \u0142 - unicode for ł

        updateUI();

    }

    public void goBack(ActionEvent actionEvent) throws IOException {
        //TODO this should not redirect to login, just a placeholder for now ~W
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login_screen.fxml"));
        return_button.getScene().setRoot(root);
    }

    public void makeReservation(ActionEvent actionEvent) {
        PersistentAlert alert = new PersistentAlert(
                AlertType.INFORMATION,
                "Dodawanie rezerwacji",
                "Prosz\u0119 czeka\u0107, dokonujemy rezerwacji...");
        alert.show();

        new Thread(() -> {
            try {
                Reservation reservation =  customer.addReservation(tour);
                if (reservation != null){
                    //reservation successful, refresh the ui with most recent data (in case there were changes)
                    Platform.runLater(() -> {
                        //run on FX Thread
                        alert.setHeaderText("Gotowe!");
                    });
                } else {
                    //there is some discrepancy in the displayed data - refresh the ui
                    Platform.runLater(() -> {
                        //run on FX Thread
                        alert.setHeaderText("Wystapi\u0142 b\u0142\u0105d. Rezerwacja nie została dokonana.");
                    });
                }
            } catch (Exception e) {
                System.err.println("Error while making reservation");
                e.printStackTrace();
                Platform.runLater(() -> {
                    //run on FX Thread
                    alert.setHeaderText("Wystapi\u0142 b\u0142\u0105d. Rezerwacja nie została dokonana.");
                });
            } finally {
                //enable closing of the alert dialog
                Platform.runLater(() -> {
                    //run on FX thread
                    updateUI();
                    alert.enableClose();
                });
            }
        }).start();
    }

    public void cancelReservation(ActionEvent actionEvent) {
        PersistentAlert alert = new PersistentAlert(
                AlertType.INFORMATION,
                "Anulowanie rezerwacji",
                "Prosz\u0119 czeka\u0107, anulujemy rezerwacj\u0119...");
        alert.show();

        new Thread(() -> {
            try {
                Reservation reservation = customer.removeLatestReservation(tour);
                if (reservation != null){
                    //reservation successful, refresh the ui with most recent data (in case there were changes)
                    Platform.runLater(() -> {
                        //run on FX Thread
                        alert.setHeaderText("Gotowe!");
                    });
                } else {
                    //there is some discrepancy in the displayed data - refresh the ui
                    Platform.runLater(() -> {
                        //run on FX Thread
                        alert.setHeaderText("Wystapi\u0142 b\u0142\u0105d. Nie uda\u0142o si\u0119 anulowa\u0107 rezerwacji.");
                    });
                }
            } catch (Exception e) {
                System.err.println("Error while making reservation");
                e.printStackTrace();
                Platform.runLater(() -> {
                    //run on FX Thread
                    alert.setHeaderText("Wystapi\u0142 b\u0142\u0105d. Nie uda\u0142o si\u0119 anulowa\u0107 rezerwacji.");
                });
            } finally {
                //enable closing of the alert dialog
                Platform.runLater(() -> {
                    //run on FX thread
                    updateUI();
                    alert.enableClose();
                });
            }
        }).start();
    }

    //fetches info about reservation/customer and updates the ui accordingly
    private void updateUI(){
        customer_loading.setText("\u0141adowanie..."); // \u0141 - unicode for uppercase ł
        customer_loading.setVisible(true);
        customer_loading.setManaged(true);
        tour_places.setText("\u0141adowanie..."); // \u0141 - unicode for uppercase ł
        hbox_customer_reservations.setVisible(false);
        hbox_reservation_controls.setVisible(false);

        new Thread(() -> {
            //fetch available/reserved places
            int availablePlaces = tour.getAvailablePlaces();
            int reservations =  customer.getReservationsForTour(tour);
            Platform.runLater(() -> {
                //run on FX thread
                setTourAvailablePlaces(availablePlaces);
                setCustomerReservedPlaces(reservations);
                showCustomerInfo();
            });
        }).start();
    }

    //must be run on the FX Thread
    private void setTourAvailablePlaces(int availablePlaces){
        if (availablePlaces < 0){
            tour_places.setText("B\u0142\u0105d"); // \u0142 - unicode for ł \u0105 - unicode for ą
        } else if (availablePlaces == 0){
            tour_places.setText("Brak wolnych miejsc");
            button_make_reservation.setDisable(true);
        } else {
            tour_places.setText(availablePlaces + "/" + tour.getMaxPlaces());
            button_make_reservation.setDisable(false);
        }
    }


    //must be run on the FX Thread
    private void setCustomerReservedPlaces(int reservedPlaces){
        if (reservedPlaces < 0){
            customer_reservations.setText("B\u0142\u0105d"); // \u0142 - unicode for ł \u0105 - unicode for ą
            button_cancel_reservation.setDisable(true);
        } else if (reservedPlaces == 0){
            customer_reservations.setText(String.format("%d", reservedPlaces));
            button_cancel_reservation.setDisable(true);
        } else {
            customer_reservations.setText(String.format("%d", reservedPlaces));
            button_cancel_reservation.setDisable(false);
        }
    }

    //must be run on the FX Thread
    private void showCustomerInfo(){
        customer_loading.setVisible(false);
        customer_loading.setManaged(false);
        hbox_customer_reservations.setVisible(true);
        hbox_reservation_controls.setVisible(true);
    }

}