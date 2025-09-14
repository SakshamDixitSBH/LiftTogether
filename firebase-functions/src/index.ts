import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

admin.initializeApp();

// Cloud Function to match riders with volunteers based on urgency and proximity
export const matchRideRequest = functions.firestore
  .document('rideRequests/{rideId}')
  .onCreate(async (snap, context) => {
    const rideRequest = snap.data();
    
    if (rideRequest.status !== 'PENDING') {
      return null;
    }
    
    try {
      // Get all available volunteers
      const volunteersSnapshot = await admin.firestore()
        .collection('volunteers')
        .where('isAvailable', '==', true)
        .where('isOnline', '==', true)
        .get();
      
      if (volunteersSnapshot.empty) {
        console.log('No available volunteers found');
        return null;
      }
      
      const volunteers = volunteersSnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      
      // Find the best match based on urgency and proximity
      const bestMatch = findBestVolunteerMatch(rideRequest, volunteers);
      
      if (bestMatch) {
        // Update the ride request with the assigned volunteer
        await snap.ref.update({
          assignedVolunteerId: bestMatch.id,
          assignedVolunteerName: bestMatch.name || 'Volunteer',
          status: 'ACCEPTED',
          acceptedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        
        // Send notification to the volunteer
        await sendNotificationToVolunteer(bestMatch.id, rideRequest);
        
        console.log(`Matched ride ${context.params.rideId} with volunteer ${bestMatch.id}`);
      }
      
    } catch (error) {
      console.error('Error matching ride request:', error);
    }
  });

// Cloud Function to send push notifications
export const sendRideNotification = functions.https.onCall(async (data, context) => {
  const { token, title, body, data: notificationData } = data;
  
  const message = {
    token: token,
    notification: {
      title: title,
      body: body
    },
    data: notificationData || {}
  };
  
  try {
    const response = await admin.messaging().send(message);
    console.log('Successfully sent message:', response);
    return { success: true, messageId: response };
  } catch (error) {
    console.error('Error sending message:', error);
    return { success: false, error: error.message };
  }
});

// Helper function to find the best volunteer match
function findBestVolunteerMatch(rideRequest: any, volunteers: any[]) {
  const urgencyPriority = {
    'EMERGENCY': 1,
    'HIGH': 2,
    'MEDIUM': 3,
    'LOW': 4
  };
  
  const rideUrgency = urgencyPriority[rideRequest.urgency as keyof typeof urgencyPriority] || 3;
  
  // Sort volunteers by distance and availability
  const sortedVolunteers = volunteers
    .map(volunteer => ({
      ...volunteer,
      distance: calculateDistance(
        rideRequest.pickupLocation.latitude,
        rideRequest.pickupLocation.longitude,
        volunteer.currentLocation.latitude,
        volunteer.currentLocation.longitude
      )
    }))
    .filter(volunteer => volunteer.distance <= volunteer.maxDistance)
    .sort((a, b) => {
      // Prioritize by urgency level first, then by distance
      if (rideUrgency <= 2) { // Emergency or High priority
        return a.distance - b.distance;
      } else {
        return a.distance - b.distance;
      }
    });
  
  return sortedVolunteers[0] || null;
}

// Helper function to calculate distance between two points
function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371; // Radius of the Earth in kilometers
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
}

// Helper function to send notification to volunteer
async function sendNotificationToVolunteer(volunteerId: string, rideRequest: any) {
  try {
    // Get volunteer's FCM token
    const volunteerDoc = await admin.firestore()
      .collection('volunteers')
      .doc(volunteerId)
      .get();
    
    const volunteer = volunteerDoc.data();
    if (!volunteer || !volunteer.fcmToken) {
      console.log('No FCM token found for volunteer:', volunteerId);
      return;
    }
    
    const message = {
      token: volunteer.fcmToken,
      notification: {
        title: 'New Ride Request',
        body: `Ride request from ${rideRequest.riderName} - ${rideRequest.urgency} priority`
      },
      data: {
        rideId: rideRequest.id,
        type: 'ride_request'
      }
    };
    
    await admin.messaging().send(message);
    console.log('Notification sent to volunteer:', volunteerId);
    
  } catch (error) {
    console.error('Error sending notification to volunteer:', error);
  }
}
